package com.picelo.endpoint.security;

import com.picelo.endpoint.service.userdetails.ReactiveUserAccessService;
import com.picelo.endpoint.service.userdetails.UserAccessModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JWTAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveUserAccessService reactiveUserAccessService;

    @Autowired
    public JWTAuthenticationManager(final ReactiveUserAccessService reactiveUserAccessService) {
        this.reactiveUserAccessService = reactiveUserAccessService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        var usernameMono = Mono.just(authentication.getPrincipal().toString());
        return usernameMono.flatMap(reactiveUserAccessService::findByUsername)
                .filter(userDetails -> ((UserAccessModel) userDetails).isAccountLoggedIn())
                .map(userDetails -> new UsernamePasswordAuthenticationToken(userDetails.getUsername(),
                                                                            userDetails.getUsername(),
                                                                            userDetails.getAuthorities()))
                .cast(Authentication.class).defaultIfEmpty(authentication).onErrorReturn(authentication);
    }

}
