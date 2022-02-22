package com.scryer.endpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.List;

@SpringBootApplication
public class EndpointApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EndpointApplication.class);
        application.setLazyInitialization(false);
        ConfigurableApplicationContext context = application.run(args);
    }
}
