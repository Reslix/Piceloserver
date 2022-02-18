package com.scryer.endpoint.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@ComponentScan
public class DynamoDBConfiguration {

    @Bean
    private CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("logout");
    }

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
        return DynamoDbClient.builder()
                .region(ddbRegion)
                .endpointOverride(ddbLocalUri)
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
