package com.scryer.endpoint.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

/**
 * For config loaded from files/environment
 */
@Configuration
public class AppConfiguration {

    @Value("${app.cookie_domain}")
    private String cookieDomain;

    @Value("${app.port}")
    private String port;

    @Value("${app.localddb}")
    private String localddb;

    @Value("${app.locals3}")
    private String locals3;

    @Value("${app.region}")
    private String region;

    @Bean
    public String cookieDomain() {
        return this.cookieDomain;
    }

    @Bean
    public String port() {
        return this.port;
    }

    @Bean
    public URI localddb() {
        if (this.localddb != null) {
            return URI.create(this.localddb);
        }
        return null;
    }

    @Bean
    public URI locals3() {
        if (this.localddb != null) {
            return URI.create(this.locals3);
        }
        return null;
    }

    @Bean
    public Region region() {
        return Region.of(this.region);
    }

    @Bean
    public String keystorePassword() {
        return System.getenv("KEYSTORE_PASSWORD");
    }

    @Bean
    public String awsAccessKeyId() {
        return System.getenv("AWS_ACCESS_KEY_ID");
    }

    @Bean
    public String awsSecretAccessKey() {
        return System.getenv("AWS_SECRET_ACCESS_KEY");
    }
}
