package com.scryer.endpoint.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class JWTSecurityContextRepository implements ServerSecurityContextRepository {

    @Autowired
    private JWTAuthenticationManager apiAuthenticationProvider;

    /**
     * We want to obtain a new security context per request so no need to save.
     *
     * @param exchange
     * @param context
     * @return
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    /**
     * Given the username extracted by the authentication manager, we embed it into the
     * security context.
     *
     * @param exchange
     * @return
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication).map(Objects::toString);
        ServerHttpRequest httpRequest = exchange.getRequest();
        HttpCookie accessCookie = httpRequest.getCookies().toSingleValueMap().get("accessToken");
        HttpCookie refreshCookie = httpRequest.getCookies().toSingleValueMap().get("refreshToken");

        if (accessCookie != null) {
            return apiAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(accessCookie.getValue(),
                                                                                                  refreshCookie.getValue()))
                                            .map(SecurityContextImpl::new);
        } else {
            return Mono.empty();
        }
    }
}
