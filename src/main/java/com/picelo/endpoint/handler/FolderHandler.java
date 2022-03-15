package com.picelo.endpoint.handler;

import com.picelo.endpoint.service.folder.FolderService;
import com.picelo.endpoint.security.JWTManager;
import com.picelo.endpoint.service.imagesrc.ImageService;
import com.picelo.endpoint.service.folder.Folder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class FolderHandler {

    private final FolderService folderService;
    private final ImageService imageService;
    private final JWTManager jwtManager;

    public FolderHandler(final FolderService folderService,
                         final ImageService imageService,
                         final JWTManager jwtManager) {
        this.folderService = folderService;
        this.imageService = imageService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getFolderById(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(folderService.getFolderById(folderId), Folder.class));
    }

    public Mono<ServerResponse> getFolderMapByUserId(final ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        return folderService.getFoldersMapByUserId(userId).flatMap(folderMap -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(folderMap)));
    }

    public Mono<ServerResponse> postNewFolder(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);

        var newFolder = serverRequest.bodyToMono(FolderService.NewFolderRequest.class)
                .filter(folder -> userId.id().equals(folder.userId()))
                .flatMap(folderService::createFolder).cache();
        var updatedParent = newFolder.flatMap(folderService::addChildToParent);

        return updatedParent.then(newFolder)
                .map(folderModel -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

    /**
     * This one is absolutely gross
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> moveFolder(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var folderId = serverRequest.pathVariable("folderId");
        var folderMono = serverRequest.bodyToMono(Folder.class)
                .filter(folder -> userId.id().equals(folder.getUserId())).cache();
        var newParentFolderIdsMono = folderService.getFolderById(folderId).flatMap(folder -> {
            var newParentFolders = folder.getFolders();
            var newParentIds = folder.getParentFolderIds();
            newParentIds.add(folder.getId());
            return folderMono.map(child -> {
                newParentFolders.add(child.getId());
                return folderService.updateFolder(Folder.builder()
                                                          .id(folder.getId())
                                                          .folders(newParentFolders)
                                                          .build());
            }).then(Mono.just(newParentIds));
        });
        var oldParentFolderMono = folderMono
                .flatMap(folderModel -> {
                    var parentFolder = folderService
                            .getFolderById(folderModel.getParentFolderIds()
                                                   .get(folderModel.getParentFolderIds().size() - 1));
                    return parentFolder.flatMap(parent -> folderService
                            .updateFolder(Folder.builder()
                                                  .id(parent.getId())
                                                  .folders(parent.getFolders()
                                                                   .stream()
                                                                   .filter(id -> !id.equals(folderModel.getId()))
                                                                   .toList())
                                                  .build()));
                });

        return oldParentFolderMono.then(folderMono)
                .zipWith(newParentFolderIdsMono)
                .map(t2 -> Folder.builder().id(t2.getT1().getId()).parentFolderIds(t2.getT2()).build())
                .flatMap(folderService::updateFolder)
                .flatMap(folderModel -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .switchIfEmpty(ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN)
                                       .body(BodyInserters.fromValue("Operation for wrong user")));
    }

    public Mono<ServerResponse> deleteFolder(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var verifiedFolderMono = serverRequest.bodyToMono(Folder.class)
                .filter(folder -> userId.id().equals(folder.getUserId())).cache();
        var parentFolderMono = verifiedFolderMono
                .flatMap(folder -> folderService
                        .getFolderById(folder.getParentFolderIds().get(folder.getParentFolderIds().size() - 1)))
                .cache();
        var updatedFolderImagesFlux = verifiedFolderMono.map(Folder::getId)
                .flatMapMany(imageService::getImageSrcForFolder).flatMap(imageService::deleteImage);

        var newParentFolderFoldersMono = Mono.zip(verifiedFolderMono, parentFolderMono,
                                                  (child, parent) -> parent.getFolders()
                                                          .stream()
                                                          .filter(id -> !id.equals(child.getId()))
                                                          .toList());
        var updatedParentFolderMono = Mono.zip(parentFolderMono, newParentFolderFoldersMono,
                                               (folder, newChildren) -> folderService
                                                       .updateFolder(Folder.builder()
                                                                             .id(folder.getId())
                                                                             .folders(newChildren)
                                                                             .build()));
        return updatedFolderImagesFlux.then(updatedParentFolderMono).then(verifiedFolderMono)
                .flatMap(folderService::deleteFolder)
                .flatMap(folderModel -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .switchIfEmpty(ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN)
                                       .body(BodyInserters.fromValue("Operation for wrong user")));
    }
}
