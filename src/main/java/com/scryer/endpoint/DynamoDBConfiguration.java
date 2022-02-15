package com.scryer.endpoint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
public class DynamoDBConfiguration {

    @Bean
    Region region() {
        return Region.US_WEST_1;
    }

    @Bean("endpointUri")
    URI localUri() {
        return URI.create("http://localhost:8000");
    }

    @Bean
    DynamoDbAsyncClient dynamoDbClient(final Region region, @Qualifier("endpointUri") final URI endpointUri) {
        return DynamoDbAsyncClient.builder()
                .region(region)
                .endpointOverride(endpointUri)
                .build();
    }

    @Bean
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(final DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
    }

    @Bean
    DynamoDbAsyncWaiter dynamoDbWaiter(final DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient.waiter();
    }
}
