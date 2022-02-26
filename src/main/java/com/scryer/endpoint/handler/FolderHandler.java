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

    public Mono<ServerResponse> getFolderById(final ServerRequest request) {
        String folderId = request.pathVariable("folderId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(folderService.getFolderByIdFromTable(folderId), FolderModel.class));
    }

    public Mono<ServerResponse> getFolderMapByUserId(final ServerRequest request) {
        String userId = request.pathVariable("userId");
        System.out.println("user1:" + userId);
        return folderService.getFoldersMapByUserIdFromTable(userId).flatMap(folderMap -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(folderMap)));
    }

    public Mono<ServerResponse> postNewFolder(final ServerRequest request) {
        return request.bodyToMono(FolderService.NewFolderRequest.class)
                .filter(folder -> jwtManager.getUserIdentity(request).id().equals(folder.userId()))
                .flatMap(folderService::createFolderInTable)
                .map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()

                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> putUpdateFolder(final ServerRequest request) {
        return request.bodyToMono(FolderModel.class)
                .filter(folder -> Long.valueOf(request.pathVariable("folderId")).equals(folder.getId()))
                .filter(folder -> jwtManager.getUserIdentity(request).id().equals(folder.getUserId()))
                .flatMap(folderService::updateFolderTable)
                .map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()

                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> deleteFolder(final ServerRequest request) {
        return request.bodyToMono(FolderModel.class)
                .filter(folder -> Long.valueOf(request.pathVariable("folderId")).equals(folder.getId()))
                .filter(folder -> jwtManager.getUserIdentity(request).id().equals(folder.getUserId()))
                .flatMap(folderService::deleteFolderFromTable)
                .map(folderModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(folderModel)))
                .defaultIfEmpty(ServerResponse.badRequest()

                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(BodyInserters.fromValue("Operation for wrong user")))
                .flatMap(response -> response);
    }

}
