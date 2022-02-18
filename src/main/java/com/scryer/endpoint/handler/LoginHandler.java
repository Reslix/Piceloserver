package com.scryer.endpoint.handler;

import com.scryer.util.JWTTokenUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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

    @Autowired
    private ReactiveUserDetailsHandler reactiveUserDetailsHandler;

    @Autowired
    private CacheManager cacheManager;

    public Mono<ServerResponse> login(final ServerRequest request) {
        Mono<String> usernameMono = ReactiveSecurityContextHolder.getContext()
                                                                 .map(SecurityContext::getAuthentication)
                                                                 .map(Principal::getName);
        return usernameMono.flatMap(username -> {
                                        cacheManager.getCache("logout").evictIfPresent(username);
                                        return ServerResponse.ok()
                                                             .cookie(ResponseCookie.from("accessToken",
                                                                                         JWTTokenUtility.createJwtAccess(
                                                                                                 username))
                                                                                   .build())
                                                             .cookie(ResponseCookie.from("refreshToken",
                                                                                         JWTTokenUtility.createJwtRefresh(
                                                                                                 username))
                                                                                   .build())
                                                             .build();
                                    }
        );
    }

    /**
     * There is some highly questionable stuff going on here that should probably be replaced by reactive redis.
     * @param request
     * @return
     */
    public Mono<ServerResponse> logout(final ServerRequest request) {
        Mono<String> usernameMono = ReactiveSecurityContextHolder.getContext()
                                                                 .map(SecurityContext::getAuthentication)
                                                                 .map(Principal::getName);
        return usernameMono.flatMap(reactiveUserDetailsHandler::findByUsername)
                           .map(userDetails -> {
                               cacheManager.getCache("logout").put(userDetails.getUsername(), true);
                               return userDetails;
                           }).flatMap(userDetails -> ServerResponse.ok().build());

    }
}
