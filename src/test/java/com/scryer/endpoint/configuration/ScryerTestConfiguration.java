package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.net.URI;
import java.time.Duration;

@TestConfiguration
public class ScryerTestConfiguration {

    @Bean
    public DockerComposeContainer container() {
        var container = new DockerComposeContainer(new File("./src/test/resources/docker-compose.yml")).withBuild(true)
                .withLocalCompose(true).withOptions("--compatibility")
                .withExposedService("dynamodb_1", 8000,
                                    Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("s3_1", 9000, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("redis_1", 6379,
                                    Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
        container.start();
        return container;
    }

    @Bean
    public String ddbEndpoint(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("dynamodb_1", 8000) + ":"
               + container.getServicePort("dynamodb_1", 8000);
    }

    @Bean
    public String s3Endpoint(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("s3_1", 9000) + ":" + container.getServicePort("s3_1", 9000);
    }

    @Bean
    public String redisEndpoint(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("redis_1", 6379) + ":" + container.getServicePort("redis_1", 6379);
    }

}
