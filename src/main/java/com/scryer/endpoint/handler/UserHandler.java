package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.FolderService;
import com.scryer.endpoint.service.ReactiveUserDetailsService;
import com.scryer.endpoint.service.UserService;
import com.scryer.model.ddb.FolderModel;
import com.scryer.model.ddb.UserModel;
import com.scryer.model.handler.CredentialCheckBoolean;
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
    private final ReactiveUserDetailsService userSecurityService;
    private final FolderService folderService;
    private final JWTManager jwtManager;

    @Autowired
    public UserHandler(final UserService userService,
                       final ReactiveUserDetailsService userSecurityService,
                       final FolderService folderService,
                       final JWTManager jwtManager) {
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.folderService = folderService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> getUserByUsername(final ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        return userService.getUserByUsername(username)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(user)));
    }

    public Mono<ServerResponse> getCheckUsername(final ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        return userService
                .getUserByUsername(username)
                .map(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(false))))
                .defaultIfEmpty(ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(true))))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> getCheckEmail(final ServerRequest serverRequest) {
        var email = serverRequest.pathVariable("email");
        return userService
                .getUserByEmail(email)
                .map(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(false))))
                .defaultIfEmpty(ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(true))))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> postNewUser(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::username);

        var requestRecord = serverRequest.bodyToMono(UserService.NewUserRequest.class);

        var validRecord = usernameMono.then(requestRecord.filter(this::validateNewUserRequest));

        return validRecord.flatMap(record -> {
            var validUserId = userService.getUniqueId(record.username(), record.email())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to generate user ID")));

            var folderMono = validUserId
                    .map(UserModel::getId)
                    .flatMap(folderService::createRootFolder)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to receive folder")));

            var folderIdMono = folderMono.map(FolderModel::getId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to extract folder ID")));

            var userMono = Mono.zip(validUserId, folderIdMono)
                    .flatMap(data -> userService.addUser(record, data.getT1().getId(), data.getT2()))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to create user table")));

            return userMono.flatMap(userModel -> {
                var userSecurityMono = userSecurityService.addUserSecurity(record, userModel.getId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User credentials already set")));
                return userSecurityMono.flatMap(mono -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(userModel))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Error creating user")))
                );
            });
        }).switchIfEmpty(Mono.error(new IllegalArgumentException("Username already taken")));
    }

    public Mono<ServerResponse> updateUser(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::username);
        return usernameMono.flatMap(username -> serverRequest.bodyToMono(UserModel.class)
                        .filter(userModel -> username.equals(userModel.getUsername()))
                        .flatMap(userService::updateUser)
                        .flatMap(userModel -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(userModel)))
                        .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_ACCEPTABLE).build()))
                .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
    }

    private boolean validateNewUserRequest(final UserService.NewUserRequest newUser) {
        return !newUser.username().isEmpty()
               && !newUser.email().isEmpty()
               && !newUser.displayName().isEmpty()
               && !newUser.password().isEmpty()
               && !newUser.firstName().isEmpty()
               && !newUser.lastName().isEmpty()
               && userService.getUserByUsername(newUser.username()).block() == null;
    }
}
