package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * For config loaded from files/environment
 */
@Configuration
public class ConfigConfiguration {

    @Value("${app.domain}")
    private String domain;

    @Value("${app.port}")
    private String port;

    @Bean
    public String domain() {
        return this.domain;
    }

    @Bean
    public String port() {
        return this.port;
    }

    @Bean
    public String jwtSignature() {
        return System.getenv("JWT_SECRET");
    }

    @Bean
    public String awsAccessKeyId() {
        return System.getenv("AWS_ACCESS_KEY_ID");

    }
    @Bean
    public String awsSecretAccessKey() {
        return System.getenv("AWS_SECRET_ACCESS_KEY");
    }
}
