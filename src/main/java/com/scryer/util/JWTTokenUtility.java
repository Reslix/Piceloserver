package com.scryer.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class JWTTokenUtility {
    private static final KeyPair KEY_PAIR = Keys.keyPairFor(SignatureAlgorithm.RS256);

    public static String createJwtAccess(final String username) {
        return Jwts.builder()
                   .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.RS256)
                   .setSubject(username)
                   .setIssuer("identity")
                   .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(2))))
                   .setIssuedAt(Date.from(Instant.now()))
                   .compact();
    }
    public static String createJwtRefresh(final String username) {
        return Jwts.builder()
                   .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.RS256)
                   .setSubject(username)
                   .setIssuer("identity")
                   .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(1))))
                   .setIssuedAt(Date.from(Instant.now()))
                   .compact();
    }

    public static boolean isTokenExpired(final Jws<Claims> token) {
        return token.getBody().getExpiration().before(Date.from(Instant.now()));
    }
    public static Jws<Claims> validateJwt(final String jwt) {
        return Jwts.parserBuilder().setSigningKey(KEY_PAIR.getPublic())
                .build()
                .parseClaimsJws(jwt);
    }
}
