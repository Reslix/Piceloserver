package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.FolderService;
import com.scryer.model.ddb.FolderModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class FolderHandler {
    private final FolderService folderService;
    private final JWTManager jwtManager;

    public FolderHandler(final FolderService folderService,
                         final JWTManager jwtManager) {
        this.folderService = folderService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getFolderById(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(folderService.getFolderById(folderId), FolderModel.class));
    }

    public Mono<ServerResponse> getFolderMapByUserId(final ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        return folderService.getFoldersMapByUserId(userId).flatMap(folderMap -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(folderMap)));
    }

    public Mono<ServerResponse> postNewFolder(final ServerRequest serverRequest) {
        var newFolder = serverRequest.bodyToMono(FolderService.NewFolderRequest.class)
                .filter(folder -> jwtManager.getUserIdentity(serverRequest).id().equals(folder.userId()))
                .flatMap(folderService::createFolder).cache();
        var updatedParent = newFolder.flatMap(folderService::addChildToParent);

        return updatedParent.then(newFolder).map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> putUpdateFolder(final ServerRequest serverRequest) {
        return serverRequest.bodyToMono(FolderModel.class)
                .filter(folder -> Long.valueOf(serverRequest.pathVariable("folderId")).equals(folder.getId()))
                .filter(folder -> jwtManager.getUserIdentity(serverRequest).id().equals(folder.getUserId()))
                .flatMap(folderService::updateFolder)
                .map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> deleteFolder(final ServerRequest serverRequest) {
        return serverRequest.bodyToMono(FolderModel.class)
                .filter(folder -> Long.valueOf(serverRequest.pathVariable("folderId")).equals(folder.getId()))
                .filter(folder -> jwtManager.getUserIdentity(serverRequest).id().equals(folder.getUserId()))
                .flatMap(folderService::deleteFolder)
                .map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

}
