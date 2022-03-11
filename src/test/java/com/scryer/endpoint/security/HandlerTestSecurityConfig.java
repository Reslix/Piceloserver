package com.scryer.endpoint.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

@TestConfiguration
@Order(1)
public class HandlerTestSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http) {
        return http.securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/**"))
                .csrf()
                .disable()
                .formLogin()
                .disable()
                .httpBasic()
                .disable()
                .authorizeExchange(exchange -> {
                    exchange.anyExchange().permitAll();
                })
                .logout()
                .disable()
                .build();
    }

}
