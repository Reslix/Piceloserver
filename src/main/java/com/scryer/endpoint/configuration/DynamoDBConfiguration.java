package com.scryer.endpoint.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@ComponentScan
public class DynamoDBConfiguration {

    @Bean
    private Region ddbRegion() {
        return Region.US_WEST_1;
    }

    @Bean
    private URI ddbLocalUri() {
        return URI.create("http://localhost:8000");
    }

    @Bean
    public DynamoDbClient dynamoDbClient(final Region ddbRegion, final URI ddbLocalUri) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create("fake", "fake");
        return DynamoDbClient.builder()
                .region(ddbRegion)
                .endpointOverride(ddbLocalUri)
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedAsyncClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }
}
