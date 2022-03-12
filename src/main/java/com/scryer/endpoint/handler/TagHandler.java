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

    private final JWTManager jwtManager;

    public TagHandler(final TagService tagService, final ImageService imageService, final JWTManager jwtManager) {
        this.tagService = tagService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getTagByName(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        var name = serverRequest.pathVariable("tagName");
        return tagService.getTag(name, userId).map(
                        tag -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(tag)))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> getUserTags(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        return tagService.getUserTags(userId).collectList().flatMap(list -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(list)));
    }


    public Mono<ServerResponse> updateImageSrcTags(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        var requestMono = serverRequest.bodyToMono(UpdateImageSrcTagsRequest.class).cache();
        var imageIdsMono = requestMono.map(UpdateImageSrcTagsRequest::imageIds);
        var tagsMono = requestMono.map(UpdateImageSrcTagsRequest::tags);
        var existingTagsFlux = tagsMono.flatMapIterable(list -> list)
                .flatMap(tag -> tagService.getTag(tag, userId));
        var newTagsFlux = tagsMono.flatMapIterable(list -> list)
                .filterWhen(tag -> tagService.getTag(tag, userId)
                        .map(t -> false)
                        .defaultIfEmpty(true))
                .flatMap(tag -> tagService.addNewTag(tag, userId));
        var tagsFlux = Flux.concat(existingTagsFlux, newTagsFlux);
        var updatedTagsMono = Flux.zip(tagsFlux, imageIdsMono.repeat(), tagService::updateTagImageIds)
                .flatMap(i -> i)
                .collectList();

        return updatedTagsMono.flatMap(tags -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(tags)));
    }

    record UpdateImageSrcTagsRequest(List<String> imageIds, List<String> tags) {
    }

    public Mono<ServerResponse> deleteImageSrcTags(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        var requestMono = serverRequest.bodyToMono(DeleteTagImageSrcsRequest.class).cache();
        var tagNamesMono = requestMono.map(DeleteTagImageSrcsRequest::tags);
        var imageIdsMono = requestMono.map(DeleteTagImageSrcsRequest::imageIds).cache();
        var tagsFlux = tagNamesMono.flatMapIterable(tags-> tags).flatMap(tag -> tagService.getTag(tag, userId)).cache();
        var deletedTagsMono = Flux.zip(tagsFlux, imageIdsMono.repeat(), tagService::deleteTagImages)
                .flatMap(tag -> tag)
                .filter(tag -> tag.getImageRankingIds().size() == 0 && tag.getImageIds().size() == 0)
                .collectList();

        return deletedTagsMono.flatMap(tags -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(tags)));
    }

    record DeleteTagImageSrcsRequest(List<String> imageIds, List<String> tags) {
    }

}
