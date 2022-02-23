package com.scryer.endpoint.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
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

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private String cookieDomain;

    @Autowired
    private JWTManager jwtManager;


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
        System.out.println("------------------ entered security context --------------------");
        System.out.println(exchange.getRequest().getQueryParams());
        System.out.println(exchange.getRequest().getCookies().toSingleValueMap());
        System.out.println(exchange.getRequest().getHeaders());
        if (exchange.getRequest().getCookies().containsKey("accessToken") &&
            exchange.getRequest().getCookies().containsKey("refreshToken")) {
            ServerHttpRequest httpRequest = exchange.getRequest();
            try {
                HttpCookie accessCookie = httpRequest.getCookies().toSingleValueMap().get("accessToken");
                Jws<Claims> accessToken = jwtManager.validateJwt(accessCookie.getValue());
            } catch (ExpiredJwtException | SignatureException e1) {
                try {
                HttpCookie refreshCookie = httpRequest.getCookies().toSingleValueMap().get("refreshToken");
                Jws<Claims> refreshToken = jwtManager.validateJwt(refreshCookie.getValue());
                    var newAccessCookie = ResponseCookie.from("accessToken",
                                                              jwtManager.createJwtAccess(refreshToken.getBody()
                                                                                                      .getSubject(),
                                                                                         refreshToken.getBody()
                                                                                                      .get(Claims.ID)
                                                                                                      .toString()))
                            .domain(cookieDomain)
                            .httpOnly(true)
                            .path("/")
                            .build();
                    var newRefreshCookie = ResponseCookie.from("refreshToken",
                                                               jwtManager.createJwtRefresh(refreshToken.getBody()
                                                                                                        .getSubject(),
                                                                                           refreshToken.getBody()
                                                                                                        .get(Claims.ID)
                                                                                                        .toString()))
                            .domain(cookieDomain)
                            .httpOnly(true)
                            .path("/")
                            .build();
                    exchange.getResponse()
                            .addCookie(newAccessCookie);
                    exchange.getResponse()
                            .addCookie(newRefreshCookie);

                } catch (ExpiredJwtException | SignatureException e2) {
                    return Mono.empty();
                }
            }
            HttpCookie accessCookie = httpRequest.getCookies().toSingleValueMap().get("accessToken");
            HttpCookie refreshCookie = httpRequest.getCookies().toSingleValueMap().get("refreshToken");

            Jws<Claims> accessToken = jwtManager.validateJwt(accessCookie.getValue());
            Jws<Claims> refreshToken = jwtManager.validateJwt(refreshCookie.getValue());
            return apiAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(
                            accessToken.getBody()
                                    .getSubject(),
                            refreshToken.getBody()
                                    .getSubject()))
                    .map(SecurityContextImpl::new);
        }
        return Mono.empty();
    }
}
