package com.scryer.endpoint.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

@TestConfiguration
@Order(1)
public class HandlerTestSecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http) {
        return http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/**"))
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .authorizeExchange(exchange -> {
                    exchange.anyExchange().permitAll();
                })
                .logout().disable()
                .build();
    }

}
