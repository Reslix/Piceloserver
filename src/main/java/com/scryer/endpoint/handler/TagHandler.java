package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.imagesrc.ImageService;
import com.scryer.endpoint.service.tag.TagService;
import com.scryer.endpoint.service.user.UserService;
import com.scryer.endpoint.service.imagesrc.ImageSrcModel;
import com.scryer.endpoint.service.tag.TagModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TagHandler {

    private final TagService tagService;

    private final ImageService imageService;

    private final JWTManager jwtManager;

    public TagHandler(final TagService tagService, final ImageService imageService, final JWTManager jwtManager) {
        this.tagService = tagService;
        this.imageService = imageService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getTagByName(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        String name = serverRequest.pathVariable("tagName");
        return tagService.getTag(name, userIdentity.id()).map(
                        tag -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(tag)))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> getUserTags(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        String userId = serverRequest.pathVariable("userId");
        return tagService.getUserTags(userIdentity.id()).collectList().flatMap(list -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(list)));
    }

    public Mono<ServerResponse> updateTagImageSrcs(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        var userId = userIdentity.id();
        var username = userIdentity.username();

        var requestMono = serverRequest.bodyToMono(UpdateTagImageSrcsRequest.class).cache();
        var imageIdsMono = requestMono.map(request -> request.imageIds).cache();
        var imageSrcsMono = imageIdsMono.flatMapIterable(list -> list).flatMap(imageService::getImageSrc);
        var tagNameMono = requestMono.map(UpdateTagImageSrcsRequest::tag).cache();
        var tagMono = tagNameMono.flatMap(tag -> tagService.getTag(tag, userId)
                .switchIfEmpty(Mono.defer(() -> tagService.addNewTag(tag, userId)))).cache();

        var updatedTagMono = Mono.zip(tagMono, imageIdsMono, tagService::updateTagImageIds);
        var updatedImagesMono = Flux.zip(imageSrcsMono, tagMono.repeat().map(TagModel::getName).map(List::of),
                                         imageService::addTagsToImageSrc).flatMap(image -> image).collectList();

        return updatedTagMono.then(updatedImagesMono).flatMap(images -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(images)));
    }

    record UpdateTagImageSrcsRequest(List<String> imageIds, String tag) {
    }

    public Mono<ServerResponse> updateImageSrcTags(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        var userId = userIdentity.id();

        var requestMono = serverRequest.bodyToMono(UpdateImageSrcTagsRequest.class).cache();
        var imageIdMono = requestMono.map(UpdateImageSrcTagsRequest::imageId).cache();
        var imageSrcMono = imageIdMono.flatMap(imageService::getImageSrc);
        var tagsMono = requestMono.map(UpdateImageSrcTagsRequest::tags).cache();
        var existingTagsFlux = tagsMono.flatMapIterable(list -> list)
                .flatMap(tag -> tagService.getTag(tag, userId));
        var newTagsFlux = tagsMono.flatMapIterable(list -> list)
                .filterWhen(tag -> tagService.getTag(tag, userId)
                        .map(t -> false)
                        .defaultIfEmpty(true))
                .flatMap(tag -> tagService.addNewTag(tag, userId));
        var tagsFlux = Flux.concat(existingTagsFlux, newTagsFlux);
        var updatedTagsMono = Flux.zip(tagsFlux, imageIdMono.map(List::of).repeat(), tagService::updateTagImageIds)
                .collectList();
        var updatedImageSrcMono = Mono.zip(imageSrcMono, tagsMono, imageService::addTagsToImageSrc)
                .flatMap(imageSrc -> imageSrc);

        return updatedTagsMono.then(updatedImageSrcMono).flatMap(imageSrc -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(imageSrc)));
    }

    record UpdateImageSrcTagsRequest(String imageId, List<String> tags) {
    }

    public Mono<ServerResponse> deleteImageSrcTags(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        var userId = userIdentity.id();
        var username = userIdentity.username();

        var requestMono = serverRequest.bodyToMono(DeleteImageSrcTagsRequest.class).cache();
        var tagNamesMono = requestMono.map(DeleteImageSrcTagsRequest::tags).cache();
        var imageIdMono = requestMono.map(DeleteImageSrcTagsRequest::imageId).cache();
        var tagsFlux = tagNamesMono.flatMapIterable(list -> list).flatMap(tag -> tagService.getTag(tag, userId));
        var imageMono = imageIdMono.flatMap(imageService::getImageSrc);
        var deletedTagsMono = Flux.zip(tagsFlux, imageMono.repeat()
                        .map(ImageSrcModel::getId)
                        .map(List::of), tagService::deleteTagImages)
                .flatMap(tag -> tag)
                .filter(tag -> tag.getImageRankingIds().size() == 0 && tag.getImageIds().size() == 0)
                .map(TagModel::getName).collectList();
        var updatedImageMono = Mono.zip(imageMono, tagNamesMono, imageService::deleteImageSrcTags)
                .flatMap(image -> image);

        return deletedTagsMono.then(updatedImageMono).flatMap(imageSrc -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(imageSrc)));
    }

    record DeleteImageSrcTagsRequest(String imageId, List<String> tags) {
    }

    public Mono<ServerResponse> deleteTagImageSrcs(final ServerRequest serverRequest) {
        JWTManager.UserIdentity userIdentity = jwtManager.getUserIdentity(serverRequest);
        var userId = userIdentity.id();
        var username = userIdentity.username();

        var requestMono = serverRequest.bodyToMono(DeleteTagImageSrcsRequest.class).cache();
        var tagNameMono = requestMono.map(DeleteTagImageSrcsRequest::tag).cache();
        var imageIdsMono = requestMono.map(DeleteTagImageSrcsRequest::imageIds);
        var tagMono = tagNameMono.flatMap(tag -> tagService.getTag(tag, userId)).cache();
        var imageFlux = imageIdsMono.flatMapIterable(list -> list).flatMap(imageService::getImageSrc);
        var deletedTagMono = Mono.zip(tagMono, imageIdsMono, tagService::deleteTagImages).flatMap(tag -> tag)
                .filter(tag -> tag.getImageRankingIds().size() == 0 && tag.getImageIds().size() == 0)
                .map(TagModel::getName).map(List::of);
        var updatedImagesMono = Flux
                .zip(imageFlux, tagNameMono.repeat().map(List::of), imageService::deleteImageSrcTags)
                .flatMap(list -> list).collectList();

        return deletedTagMono.then(updatedImagesMono).flatMap(imageSrc -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(imageSrc)));
    }

    record DeleteTagImageSrcsRequest(List<String> imageIds, String tag) {
    }

}
