package com.scryer.endpoint.configuration;

import com.scryer.model.ddb.TagModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfiguration {

    @Autowired
    private URI localredis;

    @Autowired
    private String awsSecretAccessKey;

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, TagModel> tagRedisTemplate(final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext<String, TagModel> context = RedisSerializationContext.<String, TagModel>newSerializationContext()
                .hashKey(new StringRedisSerializer())
                .hashValue(new Jackson2JsonRedisSerializer<>(TagModel.class))
                .key(new StringRedisSerializer())
                .value(new Jackson2JsonRedisSerializer<>(TagModel.class))
                .build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(
            @Qualifier("localredis") final URI localredis,
            final String awsSecretAccessKey) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(localredis.getHost(),
                                                                                                     localredis.getPort());
        redisStandaloneConfiguration.setPassword(awsSecretAccessKey);
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }
}
