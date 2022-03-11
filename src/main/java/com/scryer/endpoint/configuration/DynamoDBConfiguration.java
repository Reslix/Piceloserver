package com.scryer.endpoint.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public DynamoDbClient dynamoDbClient(final Region region, @Qualifier("ddbUri") final URI ddbUri,
                                         @Qualifier("awsAccessKeyId") final String awsAccessKeyId,
                                         @Qualifier("awsSecretAccessKey") final String awsSecretAccessKey) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        if (ddbUri != null) {
            return DynamoDbClient.builder().region(region).endpointOverride(ddbUri)
                    .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials)).build();
        } else {
            return DynamoDbClient.builder().region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials)).build();
        }
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
