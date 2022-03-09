package com.scryer.endpoint.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.model.ddb.TagModel;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

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
