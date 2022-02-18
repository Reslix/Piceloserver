package com.scryer.endpoint.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

    @Autowired
    JWTAuthenticationManager authenticationManager;

    @Autowired
    JWTSecurityContextRepository securityContextRepository;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http) {
        return http.cors().disable()
                   .csrf().disable()
                   .formLogin().disable()
                   .httpBasic(Customizer.withDefaults())
                   .authorizeExchange(exchange -> {
                       exchange.pathMatchers("/login", "/logout").permitAll()
                               .pathMatchers(HttpMethod.OPTIONS).permitAll()
                               .anyExchange()
                               .authenticated();
                   })
                   .authenticationManager(authenticationManager)
                   .securityContextRepository(securityContextRepository)
                   .logout().disable()
                   .build();
    }
}
