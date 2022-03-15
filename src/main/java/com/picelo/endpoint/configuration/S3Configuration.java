package com.picelo.endpoint.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.net.URI;

@Configuration
public class S3Configuration {

    @Bean
    public String s3ImageBucketName() {
        return "piceloimagerepo";
    }

    @Bean
    public String s3RankingBucketName() {
        return "picelorankingrepo";
    }

    @Bean
    public S3Client s3Client(final Region region,
                             @Qualifier("s3Uri") final URI s3Uri,
                             final String s3ImageBucketName,
                             final String s3RankingBucketName,
                             @Qualifier("awsAccessKeyId") final String awsAccessKeyId,
                             @Qualifier("awsSecretAccessKey") final String awsSecretAccessKey) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        S3Client s3Client;
        if (s3Uri != null) {
            s3Client = S3Client.builder().region(region).endpointOverride(s3Uri)
                    .credentialsProvider(() -> awsBasicCredentials).build();
        } else {
            s3Client = S3Client.builder().region(region).build();
        }

        try {
            var createBucketConfiguration = CreateBucketConfiguration.builder().locationConstraint(region.id()).build();
            var createImageBucketRequest = CreateBucketRequest.builder().bucket(s3ImageBucketName)
                    .createBucketConfiguration(createBucketConfiguration).acl(BucketCannedACL.PUBLIC_READ).build();
            s3Client.createBucket(createImageBucketRequest);
            var request1 = HeadBucketRequest.builder().bucket(s3ImageBucketName).build();
            var waiterResponse1 = s3Client.waiter().waitUntilBucketExists(request1);

            var createRankingBucketRequest = CreateBucketRequest.builder().bucket(s3RankingBucketName)
                    .createBucketConfiguration(createBucketConfiguration).acl(BucketCannedACL.PUBLIC_READ).build();
            s3Client.createBucket(createRankingBucketRequest);

            var request2 = HeadBucketRequest.builder().bucket(s3RankingBucketName).build();
            var waiterResponse2 = s3Client.waiter().waitUntilBucketExists(request2);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {

        }
        return s3Client;
    }

}
