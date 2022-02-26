package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.FolderService;
import com.scryer.endpoint.service.ImageService;
import com.scryer.endpoint.service.TagService;
import com.scryer.endpoint.service.UserService;
import com.scryer.model.ddb.FolderModel;
import com.scryer.model.ddb.HasTags;
import com.scryer.model.ddb.ImageSrcModel;
import com.scryer.model.ddb.UserModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagHandler {
    private final TagService tagService;
    private final UserService userService;
    private final FolderService folderService;
    private final ImageService imageService;
    private final JWTManager jwtManager;

    public TagHandler(final TagService tagService,
                      final UserService userService,
                      final FolderService folderService,
                      final ImageService imageService,
                      final JWTManager jwtManager) {
        this.tagService = tagService;
        this.userService = userService;
        this.folderService = folderService;
        this.imageService = imageService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getTagByName(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);
        String name = request.pathVariable("tagName");
        return tagService.getTagByNameAndUserIdFromTable(name, userId.id())
                .map(tag -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(tag)))
                .flatMap(response -> response);

    }


    public Mono<ServerResponse> postNewTags(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);

        var requestFlux = request.bodyToFlux(String.class);
        var tagIsntPresentFlux = requestFlux.flatMap(tag -> tagService.isTagInTable(tag, userId.id()));
        var filteredFlux = Flux.zip(requestFlux, tagIsntPresentFlux)
                .filter(tuple -> !tuple.getT2())
                .map(Tuple2::getT1);
        var tagServiceMono = filteredFlux.flatMap(tag -> tagService.putNewTagInTable(tag, userId.id())).collectList();
        var userServiceMono = filteredFlux.collect(Collectors.toList())
                .zipWith(userService.getUserByUsernameFromTable(userId.username()))
                .flatMap(tuple -> {
                    var tags = tuple.getT1();
                    var user = tuple.getT2();
                    var newTags = getCombinedTags(user, tags);
                    var newUser = UserModel.builder()
                            .username(user.getUsername())
                            .lastModified(System.currentTimeMillis())
                            .tags(newTags)
                            .build();

                    return userService.updateUserTable(newUser);
                });
        return Mono.zip(tagServiceMono, userServiceMono, (tag, user) -> tag)
                .map(tags -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(tags)))
                .flatMap(response -> response);

    }


    public Mono<ServerResponse> updateTagsForFolder(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);
        String folderId = request.pathVariable("folderId");

        var requestTags = request.bodyToFlux(String.class);

        var tagTags = requestTags.flatMap(tag -> tagService.getTagByNameAndUserIdFromTable(tag, userId.id()))
                .map(tag -> tagService.updateTagFolderId(tag, folderId)).collectList();

        var folderTags = requestTags.collectList()
                .zipWith(folderService.getFolderByIdFromTable(folderId))
                .map(tuple -> {
                    var tags = tuple.getT1();
                    var folder = tuple.getT2();
                    var newTags = getCombinedTags(folder, tags);
                    var newFolder = FolderModel.builder()
                            .id(folder.getId())
                            .lastModified(System.currentTimeMillis())
                            .tags(newTags)
                            .build();

                    return folderService.updateFolderTable(newFolder);
                });
        return Mono.zip(tagTags, folderTags, (a, b) -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(b)))
                .flatMap(response -> response);
    }


    public Mono<ServerResponse> updateTagsForImageSrc(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);
        String imageId = request.pathVariable("imageId");

        var requestTags = request.bodyToFlux(String.class);

        var tagTags = requestTags.flatMap(tag -> tagService.getTagByNameAndUserIdFromTable(tag, userId.id()))
                .map(tag -> tagService.updateTagImageId(tag, imageId)).collectList();

        var imageTags = requestTags.collectList()
                .zipWith(imageService.getImageSrcFromTable(imageId))
                .map(tuple -> {
                    var tags = tuple.getT1();
                    var imageSrc = tuple.getT2();
                    var newTags = getCombinedTags(imageSrc, tags);
                    var newImageSrc = ImageSrcModel.builder()
                            .id(imageSrc.getId())
                            .lastModified(System.currentTimeMillis())
                            .tags(newTags)
                            .build();

                    return imageService.updateImageSrcTable(newImageSrc);
                });
        return Mono.zip(tagTags, imageTags, (a, b) -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(b)))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> deleteTags(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);

        // Delete tags from images
        // Delete tags from user
        // Delete tags itself

        return Mono.empty();

    }

    public Mono<ServerResponse> deleteTagsFromFolder(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);

        return Mono.empty();
    }

    public Mono<ServerResponse> deleteTagsFromImageSrc(final ServerRequest request) {
        JWTManager.UserId userId = jwtManager.getUserIdentity(request);

        return Mono.empty();
    }

    private <T extends HasTags> List<String> getCombinedTags(final HasTags item, final Collection<String> tags) {
        Set<String> newTags = new HashSet<>(tags.size() + item.getTags().size());
        newTags.addAll(tags);
        newTags.addAll(item.getTags());
        return List.copyOf(newTags);
    }
}
