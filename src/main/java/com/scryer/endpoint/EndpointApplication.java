package com.scryer.endpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class EndpointApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EndpointApplication.class);
        application.setLazyInitialization(false);
        ConfigurableApplicationContext context = application.run(args);
    }
}
