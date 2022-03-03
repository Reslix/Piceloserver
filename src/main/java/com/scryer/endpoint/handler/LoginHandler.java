package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.ReactiveUserDetailsService;
import com.scryer.model.ddb.UserSecurityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Cookie;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Service
public class LoginHandler {

    private final ReactiveUserDetailsService reactiveUserDetailsService;
    private final JWTManager jwtManager;
    private final CacheManager cacheManager;
    private final String cookieDomain;

    @Autowired
    public LoginHandler(final ReactiveUserDetailsService reactiveUserDetailsService,
                        final JWTManager jwtManager,
                        final CacheManager cacheManager,
                        final String cookieDomain) {
        this.reactiveUserDetailsService = reactiveUserDetailsService;
        this.jwtManager = jwtManager;
        this.cacheManager = cacheManager;
        this.cookieDomain = cookieDomain;
    }

    /**
     * Also some questionable logic for using caches
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> login(final ServerRequest serverRequest) {
        Mono<String> usernameMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .map(String::toLowerCase)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Could not get username from context")));
        Mono<String> passwordMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getCredentials)
                .cast(String.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Could not get password from context")));
        Mono<UserSecurityModel> userMono =
                usernameMono.flatMap(username -> reactiveUserDetailsService.getUser(username));

        return usernameMono.map(username -> cacheManager.getCache("logout").evictIfPresent(username))
                .then(userMono)
                .flatMap(user -> ServerResponse.ok()
                        .cookie(ResponseCookie.from("accessToken",
                                                    jwtManager.createJwtAccess(user.getUsername(),
                                                                               user.getId()))
                                        .domain(cookieDomain)
                                        .httpOnly(true)
                                        .path("/")
                                        .sameSite(Cookie.SameSite.LAX.attributeValue()).build())
                        .cookie(ResponseCookie.from("refreshToken",
                                                    jwtManager.createJwtRefresh(user.getUsername(),
                                                                                user.getId()))
                                        .domain(cookieDomain)
                                        .httpOnly(true)
                                        .path("/")
                                        .sameSite(Cookie.SameSite.LAX.attributeValue()).build())
                        .build()
                )
                .switchIfEmpty(ServerResponse.status(HttpStatus.FORBIDDEN).build());
    }

    /**
     * There is some highly questionable stuff going on here that should probably be replaced by reactive redis.
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> logout(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::username);
        return usernameMono.flatMap(reactiveUserDetailsService::findByUsername)
                .map(userDetails -> {
                    cacheManager.getCache("logout").put(userDetails.getUsername(), true);
                    return userDetails;
                }).flatMap(userDetails -> ServerResponse.ok().cookie(ResponseCookie.from("accessToken", "").build())
                        .cookie(ResponseCookie.from("refreshToken", "").build()).build());
    }
}
