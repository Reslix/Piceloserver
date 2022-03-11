package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
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
	public String s3BucketName() {
		return "scryerimagerepo";
	}

	@Bean
	public S3Client s3Client(final Region region, @Qualifier("s3Uri") final URI s3Uri, final String s3BucketName,
			@Qualifier("awsAccessKeyId") final String awsAccessKeyId,
			@Qualifier("awsSecretAccessKey") final String awsSecretAccessKey) {
		AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
		S3Client s3Client;
		if (s3Uri != null) {
			s3Client = S3Client.builder().region(region).endpointOverride(s3Uri)
					.credentialsProvider(() -> awsBasicCredentials).build();
		}
		else {
			s3Client = S3Client.builder().region(region).build();
		}

		try {
			var createBucketConfiguration = CreateBucketConfiguration.builder().locationConstraint(region.id()).build();
			var createBucketRequest = CreateBucketRequest.builder().bucket(s3BucketName)
					.createBucketConfiguration(createBucketConfiguration).acl(BucketCannedACL.PUBLIC_READ).build();
			s3Client.createBucket(createBucketRequest);
			var request = HeadBucketRequest.builder().bucket(s3BucketName).build();
			var waiterResponse = s3Client.waiter().waitUntilBucketExists(request);
		}
		catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {

		}
		return s3Client;
	}

}
