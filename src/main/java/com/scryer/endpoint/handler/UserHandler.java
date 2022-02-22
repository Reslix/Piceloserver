package com.scryer.endpoint.handler;

import com.scryer.endpoint.service.FolderService;
import com.scryer.endpoint.service.ReactiveUserDetailsService;
import com.scryer.endpoint.service.UserService;
import com.scryer.model.ddb.FolderModel;
import com.scryer.model.ddb.UserModel;
import com.scryer.model.handler.CredentialCheckBoolean;
import com.scryer.util.JWTTokenUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class UserHandler {

    private final UserService userService;
    private final ReactiveUserDetailsService userSecurityService;
    private final FolderService folderService;

    @Autowired
    public UserHandler(final UserService userService,
                       final ReactiveUserDetailsService userSecurityService,
                       final FolderService folderService) {
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.folderService = folderService;
    }

    public Mono<ServerResponse> getUserByUsername(final ServerRequest request) {
        String username = request.pathVariable("username");
        return this.userService.getUserByUsernameFromTable(username)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(user)));
    }

    public Mono<ServerResponse> getCheckUsername(final ServerRequest request) {
        String username = request.pathVariable("username");
        return this.userService
                .getUserByUsernameFromTable(username)
                .map(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(false))))
                .defaultIfEmpty(ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(true))))
                .flatMap(response -> response);
    }

    public Mono<ServerResponse> getCheckEmail(final ServerRequest request) {
        String email = request.pathVariable("email");
        return this.userService
                .getUserByEmailFromTable(email)
                .map(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(false))))
                .defaultIfEmpty(ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(new CredentialCheckBoolean(true))))
                .flatMap(response -> response);
    }

    /**
     * With a new user, we require the following information:
     * * username
     * * password
     * * email
     * * first name (optional and can be updated)
     * * last name (optional and can be updated)
     * * display name (optional and can be updated)
     * <p>
     * Fields that will be generated:
     * * create date
     * * last login
     * * rootFolderId
     * *
     *
     * @param request
     * @return
     */
    public Mono<ServerResponse> postNewUser(final ServerRequest request) {
        var usernameMono = Mono.justOrEmpty(JWTTokenUtility.getUserIdentity(request))
                .map(JWTTokenUtility.UserId::username);

        var requestRecord = request.bodyToMono(UserService.NewUserRequest.class);

        var validRecord = usernameMono.then(requestRecord.filter(this::validateNewUserRequest));

        return validRecord.flatMap(record -> {
            var validUserId = userService.getUniqueId()
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to generate user ID")));

            var folderMono = validUserId.filter(id -> {
                        System.out.println("user3:" + id);
                        return true;
                    }).flatMap(folderService::createRootFolderInTable)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to receive folder")));

            var folderIdMono = folderMono.map(FolderModel::getId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to extract folder ID")));

            var userMono = Mono.zip(validUserId, folderIdMono)
                    .flatMap(data -> {
                        System.out.println("user2:" + data.getT1());
                        return userService.addUserToTable(record, data.getT1(), data.getT2());
                    })
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to create user table")));

            return userMono.flatMap(userModel -> {
                var userSecurityMono = userSecurityService.addUserSecurityToTable(record, userModel.getId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User credentials already set")));

                System.out.println("should be creating:" + userModel.toString());

                return userSecurityMono.flatMap(mono -> {
                    System.out.println("securityMono:" + mono);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(userModel))
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Error creating user")));
                });
            });
        }).switchIfEmpty(Mono.error(new IllegalArgumentException("Username already taken")));
    }

    public Mono<ServerResponse> updateUser(final ServerRequest request) {
        var usernameMono = Mono.justOrEmpty(JWTTokenUtility.getUserIdentity(request))
                .map(JWTTokenUtility.UserId::username);
        return usernameMono.flatMap(username -> request.bodyToMono(UserModel.class)
                .filter(userModel -> username.equals(userModel.getUsername()))
                .flatMap(userService::updateUserTable)
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
               && userService.getUserByUsernameFromTable(newUser.username()).block() == null;
    }
}
