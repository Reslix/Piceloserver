package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
        var container = new DockerComposeContainer(new File("./src/test/resources/docker-compose.yml"))
                .withBuild(true)
                .withLocalCompose(true)
                .withOptions("--compatibility")
                .withExposedService("dynamodb_1", 8000, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("s3_1", 9000, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("redis_1", 6379, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
        container.start();
        return container;
    }

    @Bean
    public String localddbUrl(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("dynamodb_1", 8000) +
               ":" + container.getServicePort("dynamodb_1", 8000);
    }

    @Bean
    public String locals3Url(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("s3_1", 9000) +
               ":" + container.getServicePort("s3_1", 9000);
    }

    @Bean
    public String localredisUrl(final DockerComposeContainer container) {
        return "http://" + container.getServiceHost("redis_1", 6379) +
               ":" + container.getServicePort("redis_1", 6379);
    }

    @Autowired
    private String awsAccessKeyId;

    @Autowired
    private String awsSecretAccessKey;

    @Autowired
    private Region region;

    @Bean
    public URI locals3(final String locals3Url) {
        return URI.create(locals3Url);
    }

    @Bean
    public URI localddb(final String localddbUrl) {
        return URI.create(localddbUrl);
    }


    @Bean
    public URI localredis(final String localredisUrl) {
        return URI.create(localredisUrl);
    }
}
