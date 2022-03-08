package com.scryer.endpoint.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.DockerComposeContainer;
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

@TestConfiguration
public class ScryerTestConfiguration {

    @ClassRule
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yml")).withExposedService("dynamodb_1", 8000)
                    .withExposedService("s3_1", 9000);

    public static String localddbUrl = environment.getServiceHost("dynamodb_1", 8000) +
                                       ":" + environment.getServicePort("dynamodb_1", 8000);
    public static String locals3Url = environment.getServiceHost("s3_1", 8000) +
                                 ":" + environment.getServicePort("s3_1", 8000);
    @Autowired
    private String awsAccessKeyId;

    @Autowired
    private String awsSecretAccessKey;

    @Autowired
    private Region region;

    private URI locals3 = URI.create(locals3Url);

    private URI localddb = URI.create(localddbUrl);

    @Bean
    @Primary
    public S3Client s3Client(final Region s3Region, final URI locals3, final String s3BucketName) {
        S3Client s3Client;
        if (locals3 != null) {
            s3Client = S3Client.builder().region(s3Region).endpointOverride(locals3).build();
        } else {
            s3Client = S3Client.builder().region(s3Region).build();
        }

        try {
            var createBucketConfiguration = CreateBucketConfiguration.builder().locationConstraint(region.id()).build();
            var createBucketRequest = CreateBucketRequest.builder()
                    .bucket(s3BucketName)
                    .createBucketConfiguration(createBucketConfiguration)
                    .acl(BucketCannedACL.PUBLIC_READ)
                    .build();
            s3Client.createBucket(createBucketRequest);
            var request = HeadBucketRequest.builder().bucket(s3BucketName).build();
            var waiterResponse = s3Client.waiter().waitUntilBucketExists(request);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {

        }
        return s3Client;
    }

    @Bean
    @Primary
    public DynamoDbClient dynamoDbClient(final Region region,
                                         final URI localddb,
                                         final String awsAccessKeyId,
                                         final String awsSecretAccessKey) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        if(localddb != null) {
            return DynamoDbClient.builder()
                    .region(region)
                    .endpointOverride(localddb)
                    .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                    .build();
        }
        else {
            return DynamoDbClient.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                    .build();
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
