package com.scryer.endpoint.security;

import com.scryer.endpoint.handler.ReactiveUserDetailsHandler;
import com.scryer.model.ddb.UserSecurityModel;
import com.scryer.util.JWTTokenUtility;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

@Component
public class JWTAuthenticationManager implements ReactiveAuthenticationManager {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        var accessString = authentication.getPrincipal().toString();
        var refreshString = authentication.getCredentials().toString();
        try {
            Jws<Claims> accessToken = JWTTokenUtility.validateJwt(accessString);
            Jws<Claims> refreshToken = JWTTokenUtility.validateJwt(refreshString);

            String username = accessToken.getBody().getSubject();

            if (!(Boolean) Optional.ofNullable(cacheManager.getCache("logout").get(username))
                                   .map(Cache.ValueWrapper::get)
                                   .orElse(false)) {
                if (JWTTokenUtility.isTokenExpired(accessToken)) {
                    if (JWTTokenUtility.isTokenExpired(refreshToken)) {
                        return Mono.empty();
                    }
                }

                Authentication result = new UsernamePasswordAuthenticationToken(username, username,
                                                                                List.of(new SimpleGrantedAuthority(
                                                                                        "user")));
                return Mono.just(result);
            }
            else {
                return Mono.empty();
            }
        } catch (MalformedJwtException e) {
            return Mono.just(authentication);
        }
    }
}
