package com.scryer.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class JWTTokenUtility {
    private static final KeyPair KEY_PAIR = Keys.keyPairFor(SignatureAlgorithm.RS256);

    public static String createJwtAccess(final String username, final String userId) {
        return Jwts.builder()
                   .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.RS256)
                   .setSubject(username)
                   .claim(Claims.ID, userId)
                   .setIssuer("identity")
                   .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(8))))
                   .setIssuedAt(Date.from(Instant.now()))
                   .compact();
    }

    public static String createJwtRefresh(final String username, final String userId) {
        return Jwts.builder()
                   .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.RS256)
                   .setSubject(username)
                   .claim(Claims.ID, userId)
                   .setIssuer("identity")
                   .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(2))))
                   .setIssuedAt(Date.from(Instant.now()))
                   .compact();
    }

    public static UserId getUserIdentity(final ServerRequest request) {
        try {
            var accessTokenCookie = request.cookies().toSingleValueMap().get("accessToken").getValue();
            var jws = validateJwt(accessTokenCookie);
            return new UserId(jws.getBody().getSubject(), jws.getBody().get(Claims.ID).toString());
        } catch (NullPointerException | MalformedJwtException e) {
            return null;
        }
    }

    public static Jws<Claims> validateJwt(final String jwt) {
        return Jwts.parserBuilder().setSigningKey(KEY_PAIR.getPublic())
                   .build()
                   .parseClaimsJws(jwt);
    }

    public record UserId(String username, String id) {
    }
}
