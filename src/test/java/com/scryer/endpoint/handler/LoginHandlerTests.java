package com.scryer.endpoint.handler;

import com.scryer.endpoint.EndpointApplication;
import com.scryer.endpoint.security.WebSecurityConfig;
import com.scryer.model.ddb.UserSecurityModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.util.MultiValueMap;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnableAutoConfiguration
@SpringBootTest(classes = {EndpointApplication.class, WebSecurityConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoginHandlerTests {

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
                .password(passwordEncoder.encode("test"))
                .authorities(List.of(new SimpleGrantedAuthority("user")))
                .accountNonExpired(true)
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        when(userSecurityModelTable.getItem(any(Key.class)))
                .thenReturn(userSecurityModel);

        MultiValueMap<String, ResponseCookie> cookies =
                webTestClient.post()
                        .uri("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " +
                                                           Base64Utils.encodeToString("test:test".getBytes(
                                                                   StandardCharsets.UTF_8)))
                        .exchange()
                        .expectStatus().isOk()
                        .returnResult(Object.class).getResponseCookies();


    }

    @Test
    void testUnsecureApi() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/hello")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden();
    }
}
