package com.scryer.integ;

import com.scryer.endpoint.EndpointApplication;
import com.scryer.endpoint.Greeting;
import com.scryer.model.ddb.UserSecurityModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.util.MultiValueMap;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.nio.charset.StandardCharsets;

@EnableAutoConfiguration
@SpringBootTest(classes = {EndpointApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScryerIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CacheManager cacheManager;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @MockBean
    private DynamoDbTable<UserSecurityModel> userSecurityModelTable;

    @Test
    void testLogin() {
        UserSecurityModel userSecurityModel = UserSecurityModel.builder()
                                                               .username("test")
                                                               .passwordHash(passwordEncoder.encode("test"))
                                                               .build();
        Mockito.when(userSecurityModelTable.getItem(ArgumentMatchers.any(Key.class)))
               .thenReturn(userSecurityModel);
        webTestClient.get()
                     .uri("/api/hello")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isUnauthorized();

        MultiValueMap<String, ResponseCookie> cookies =
                webTestClient.post()
                             .uri("/login")
                             .header(HttpHeaders.AUTHORIZATION, "Basic " +
                                                                Base64Utils.encodeToString("test:test".getBytes(
                                                                        StandardCharsets.UTF_8)))
                             .exchange()
                             .expectStatus().isOk()
                             .returnResult(Object.class).getResponseCookies();

        webTestClient.get()
                     .uri("/api/hello")
                     .cookie("accessToken", cookies.toSingleValueMap().get("accessToken").getValue())
                     .cookie("refreshToken", cookies.toSingleValueMap().get("refreshToken").getValue())
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk();

        webTestClient.get()
                     .uri("/api/hello")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isUnauthorized();

        webTestClient.post()
                     .uri("/logout")
                     .cookie("accessToken", cookies.toSingleValueMap().get("accessToken").getValue())
                     .cookie("refreshToken", cookies.toSingleValueMap().get("refreshToken").getValue())
                     .exchange()
                     .expectStatus().isOk();

        webTestClient.get()
                     .uri("/api/hello")
                     .cookie("accessToken", cookies.toSingleValueMap().get("accessToken").getValue())
                     .cookie("refreshToken", cookies.toSingleValueMap().get("refreshToken").getValue())
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isUnauthorized();
    }
}
