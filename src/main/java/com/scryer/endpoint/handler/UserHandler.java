package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.folder.FolderService;
import com.scryer.endpoint.service.userdetails.ReactiveUserAccessService;
import com.scryer.endpoint.service.user.UserService;
import com.scryer.endpoint.service.folder.FolderModel;
import com.scryer.endpoint.service.user.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class UserHandler {

    private final UserService userService;

    private final ReactiveUserAccessService userSecurityService;

    private final FolderService folderService;

    private final JWTManager jwtManager;

    @Autowired
    public UserHandler(final UserService userService, final ReactiveUserAccessService userSecurityService,
                       final FolderService folderService, final JWTManager jwtManager) {
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.folderService = folderService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getUserByUsername(final ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        return userService.getUserByUsername(username).flatMap(
                        user -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(user)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getCheckUsername(final ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        return userService.getUserByUsername(username)
                .map(user -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(false)))
                .defaultIfEmpty(
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(true)))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> getCheckEmail(final ServerRequest serverRequest) {
        var email = serverRequest.pathVariable("email");
        return userService.getUserByEmail(email)
                .map(user -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(false)))
                .defaultIfEmpty(
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(true)))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> postNewUser(final ServerRequest serverRequest) {
        var requestRecord = serverRequest.bodyToMono(UserService.NewUserRequest.class).cache();

        var validRecord = requestRecord.filterWhen(userService::validateUser);

        return validRecord.flatMap(record -> {
            var validUserId = userService.getUniqueId(record.username(), record.email());

            var folderMono = validUserId.map(UserModel::getId).flatMap(folderService::createRootFolder);

            var folderIdMono = folderMono.map(FolderModel::getId);

            var userMono = Mono.zip(validUserId, folderIdMono)
                    .flatMap(data -> userService.addUser(record, data.getT1().getId(), data.getT2()));

            return userMono.flatMap(userModel -> {
                var userSecurityMono = userSecurityService.addUserSecurity(record, userModel.getId());
                return userSecurityMono.flatMap(mono -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(userModel)));
            });
        }).switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }

    public Mono<ServerResponse> updateUser(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::username);
        return usernameMono
                .flatMap(username -> serverRequest.bodyToMono(UserModel.class)
                        .filter(userModel -> username.equals(userModel.getUsername())).flatMap(userService::updateUser)
                        .flatMap(userModel -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(userModel)))
                        .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_ACCEPTABLE).build()))
                .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
    }

}
