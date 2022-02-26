package com.scryer.endpoint.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

    @Autowired
    private JWTAuthenticationManager jwtAuthenticationManager;

    @Autowired
    private JWTSecurityContextRepository jwtSecurityContextRepository;

    @Autowired
    private ReactiveUserDetailsService reactiveUserDetailsService;

    @Bean
    ReactiveAuthenticationManager authenticationManager() {
        return new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOriginPatterns(List.of("*"));
        corsConfiguration.setAllowedMethods(List.of("*"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    AuthenticationWebFilter authenticationWebFilter(final ReactiveAuthenticationManager authenticationManager,
                                                    final ReactiveUserDetailsService reactiveUserDetailsService) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(new ServerHttpBasicAuthenticationConverter());
        return filter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChainAuth(final ServerHttpSecurity http,
                                                             final CorsConfigurationSource source,
                                                             final AuthenticationWebFilter authenticationWebFilter,
                                                             final ReactiveAuthenticationManager authenticationManager) {
        return http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/auth/**"))
                .cors().configurationSource(source).and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic()
                .authenticationEntryPoint((serverWebExchange, exception) -> Mono.fromRunnable(() -> {
                    serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                })).and()
                .authenticationManager(authenticationManager)
                .exceptionHandling(exceptionHandlingSpec -> {
                    exceptionHandlingSpec
                            .authenticationEntryPoint((serverWebExchange, exception) -> Mono.fromRunnable(() -> {
                                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            }))
                            .accessDeniedHandler((serverWebExchange, exception) -> Mono.fromRunnable(() -> {
                                serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            }));
                })
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers("/auth/login").authenticated();
                    exchange.pathMatchers("/auth/**").permitAll().anyExchange().authenticated();
                })
                .logout().disable()
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChainAPI(final ServerHttpSecurity http,
                                                            final CorsConfigurationSource source) {
        return http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/**"))
                .cors().configurationSource(source).and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .exceptionHandling(exceptionHandlingSpec -> {
                    exceptionHandlingSpec
                            .authenticationEntryPoint((serverWebExchange, exception) -> Mono.fromRunnable(() -> {
                                System.out.println("unauthorized");
                                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            }))
                            .accessDeniedHandler((serverWebExchange, exception) -> Mono.fromRunnable(() -> {
                                System.out.println("forbidden");
                                serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            }));
                })
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers("/api/**").authenticated();
                })
                .authenticationManager(jwtAuthenticationManager)
                .securityContextRepository(jwtSecurityContextRepository)
                .logout().disable()
                .build();
    }
}
