package com.scryer.endpoint.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class JWTAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService reactiveUserDetailsService;

    @Autowired
    public JWTAuthenticationManager(final ReactiveUserDetailsService reactiveUserDetailsService) {
        this.reactiveUserDetailsService = reactiveUserDetailsService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        var usernameMono = Mono.just(authentication.getPrincipal().toString());
        return usernameMono.flatMap(reactiveUserDetailsService::findByUsername)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(userDetails.getUsername(),
                                                                            userDetails.getUsername(),
                                                                            userDetails.getAuthorities()))
                .cast(Authentication.class)
                .onErrorReturn(authentication);
    }
}
