package com.picelo.endpoint.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

/**
 * For config loaded from files/environment
 */
@Configuration
public class AppConfiguration {

	@Autowired
	private Environment env;

	@Bean
	public String cookieDomain() {
		return env.getProperty("app.cookie_domain");
	}

	@Bean
	public String port() {
		return env.getProperty("app.port");
	}

	@Bean
	public URI ddbUri() {
		var ddbEndpoint = env.getProperty("app.ddbEndpoint");
		if (ddbEndpoint != null) {
			return URI.create(ddbEndpoint);
		}
		return null;
	}

	@Bean
	public URI s3Uri() {
		var s3Endpoint = env.getProperty("app.s3Endpoint");
		if (s3Endpoint != null) {
			return URI.create(s3Endpoint);
		}
		return null;
	}

	@Bean
	public URI redisUri() {
		var redisEndpoint = env.getProperty("app.redisEndpoint");
		if (redisEndpoint != null) {
			return URI.create(redisEndpoint);
		}
		return null;
	}

	@Bean
	public Region region() {
		var region = env.getProperty("app.region");
		if (region != null) {
			return Region.of(region);
		}
		return null;
	}

	@Bean
	public String keystorePassword() {
		return env.getProperty("app.keystore.password");
	}

	@Bean
	public String awsAccessKeyId() {
		return env.getProperty("app.aws.access.key.id");
	}

	@Bean
	public String awsSecretAccessKey() {
		return env.getProperty("app.aws.secret.access.key");
	}

}
