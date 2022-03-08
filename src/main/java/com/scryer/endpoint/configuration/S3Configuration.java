package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

import java.net.URI;

@Configuration
public class S3Configuration {

    @Autowired
    private Region region;

    @Autowired
    private URI locals3;

    @Bean
    public String s3BucketName() {
        return "scryerimagerepo";
    }

    @Bean
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
}
