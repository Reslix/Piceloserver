package com.scryer.endpoint.configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;

@Configuration
public class RedisConfiguration {

    @Autowired
    private URI localredis;

    @Autowired
    private String awsSecretAccessKey;

    public ReactiveRedisConnectionFactory redisConnectionFactory(final URI localredis, final String awsSecretAccessKey) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(localredis.getHost(),
                                                                                                     localredis.getPort());
        redisStandaloneConfiguration.setPassword(awsSecretAccessKey);
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }
}
