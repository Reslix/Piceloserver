package com.scryer.endpoint.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JWTManager {
    private final KeyPair KEY_PAIR;

    @Autowired
    public JWTManager(final String keystorePassword) {
        this.KEY_PAIR = getKeyPair(keystorePassword);
    }

    public String createJwtAccess(final String username, final String userId) {
        return Jwts.builder()
                .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.forSigningKey(KEY_PAIR.getPrivate()))
                .setSubject(username)
                .claim(Claims.ID, userId)
                .setIssuer("identity")
                .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(8))))
                .setIssuedAt(Date.from(Instant.now()))
                .compact();
    }

    public String createJwtRefresh(final String username, final String userId) {
        return Jwts.builder()
                .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.forSigningKey(KEY_PAIR.getPrivate()))
                .setSubject(username)
                .claim(Claims.ID, userId)
                .setIssuer("identity")
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(2))))
                .setIssuedAt(Date.from(Instant.now()))
                .compact();
    }

    public UserId getUserIdentity(final ServerRequest request) {
        try {
            var accessTokenCookie = request.cookies().toSingleValueMap().get("accessToken").getValue();
            var jws = validateJwt(accessTokenCookie);
            return new UserId(jws.getBody().getSubject(), jws.getBody().get(Claims.ID).toString());
        } catch (NullPointerException | MalformedJwtException e) {
            return null;
        }
    }

    public Jws<Claims> validateJwt(final String jwt) {
        return Jwts.parserBuilder().setSigningKey(KEY_PAIR.getPublic())
                .build()
                .parseClaimsJws(jwt);
    }

    public record UserId(String username, String id) {
    }

    private KeyPair getKeyPair(final String keystorePassword) {
        KeyPair keyPair = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(new ClassPathResource("server.p12").getFile(),
                                                     keystorePassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) keyStore.getKey("1", keystorePassword.toCharArray());
            PublicKey publicKey = keyStore.getCertificate("1").getPublicKey();
            keyPair = new KeyPair(publicKey, privateKey);

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        if (keyPair == null) {
            keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
        }
        return keyPair;
    }
}
