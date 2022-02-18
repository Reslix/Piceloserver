package com.scryer.endpoint.security;

import com.scryer.endpoint.handler.LoginHandler;
import com.scryer.util.JWTTokenUtility;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class JWTCookieFilter implements WebFilter {
    @Autowired
    private CacheManager cacheManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Jws<Claims> accessToken = JWTTokenUtility.validateJwt(exchange.getRequest()
                                                                      .getCookies()
                                                                      .toSingleValueMap()
                                                                      .get("accessToken")
                                                                      .getValue());
        Jws<Claims> resetToken = JWTTokenUtility.validateJwt(exchange.getRequest()
                                                                     .getCookies()
                                                                     .toSingleValueMap()
                                                                     .get("resetToken")
                                                                     .getValue());
        if (JWTTokenUtility.isTokenExpired(accessToken) && !JWTTokenUtility.isTokenExpired(resetToken)) {
            if (!(Boolean) cacheManager.getCache("logout").get(accessToken.getBody().getSubject()).get()) {
                exchange.getResponse()
                        .addCookie(ResponseCookie.from("accessToken",
                                                       JWTTokenUtility.createJwtAccess(accessToken.getBody()
                                                                                                  .getSubject()))
                                                 .build());
                exchange.getResponse()
                        .addCookie(ResponseCookie.from("refreshToken",
                                                       JWTTokenUtility.createJwtRefresh(accessToken.getBody()
                                                                                                  .getSubject()))
                                                 .build());
            }
        }
        return chain.filter(exchange);
    }
}
