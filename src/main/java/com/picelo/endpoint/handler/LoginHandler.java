package com.picelo.endpoint.handler;

import com.picelo.endpoint.security.JWTManager;
import com.picelo.endpoint.service.userdetails.ReactiveUserAccessService;
import com.picelo.endpoint.service.userdetails.UserAccessModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Service
public class LoginHandler {

    private final ReactiveUserAccessService reactiveUserAccessService;

    private final JWTManager jwtManager;

    private final String cookieDomain;

    @Autowired
    public LoginHandler(final ReactiveUserAccessService reactiveUserAccessService,
                        final JWTManager jwtManager,
                        final String cookieDomain) {
        this.reactiveUserAccessService = reactiveUserAccessService;
        this.jwtManager = jwtManager;
        this.cookieDomain = cookieDomain;
    }

    public Mono<ServerResponse> login(final ServerRequest serverRequest) {
        Mono<String> usernameMono = ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .map(Principal::getName).map(String::toLowerCase)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Could not get username from context")));
        Mono<UserAccessModel> userMono = usernameMono.flatMap(reactiveUserAccessService::getUser);

        return usernameMono.then(userMono)
                .flatMap(
                        user -> reactiveUserAccessService.updateUserAccess(UserAccessModel.builder()
                                                                                   .username(user.getUsername())
                                                                                   .credentialsNonExpired(user.isCredentialsNonExpired())
                                                                                   .accountNonExpired(user.isAccountNonExpired())
                                                                                   .accountNonLocked(user.isAccountNonLocked())
                                                                                   .enabled(user.isEnabled())
                                                                                   .accountLoggedIn(true)
                                                                                   .build()))
                .flatMap(user -> ServerResponse.ok()
                        .cookie(ResponseCookie
                                        .from("accessToken",
                                              jwtManager.createJwtAccess(user.getUsername(), user.getId()))
                                        .domain(cookieDomain).httpOnly(true).path("/").sameSite(
                                        Cookie.SameSite.LAX.attributeValue())
                                        .build())
                        .cookie(ResponseCookie
                                        .from("refreshToken",
                                              jwtManager.createJwtRefresh(user.getUsername(), user.getId()))
                                        .domain(cookieDomain).httpOnly(true).path("/")
                                        .sameSite(Cookie.SameSite.LAX.attributeValue()).build())
                        .build())
                .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
    }

    public Mono<ServerResponse> logout(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::username);
        var userAccessMono = usernameMono.flatMap(reactiveUserAccessService::findByUsername).cache();
        var logoutMono = userAccessMono.map(user -> reactiveUserAccessService.updateUserAccess(UserAccessModel.builder()
                                                                                                       .username(user.getUsername())
                                                                                                       .credentialsNonExpired(
                                                                                                               user.isCredentialsNonExpired())
                                                                                                       .accountNonExpired(
                                                                                                               user.isAccountNonExpired())
                                                                                                       .accountNonLocked(
                                                                                                               user.isAccountNonLocked())
                                                                                                       .enabled(user.isEnabled())
                                                                                                       .accountLoggedIn(
                                                                                                               false)
                                                                                                       .build()));
        return logoutMono.then(usernameMono).flatMap(reactiveUserAccessService::findByUsername)
                .flatMap(userDetails -> ServerResponse.ok().cookie(ResponseCookie.from("accessToken", "").build())
                        .cookie(ResponseCookie.from("refreshToken", "").build()).build());
    }

}
