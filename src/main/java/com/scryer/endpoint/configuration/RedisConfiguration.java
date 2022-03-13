package com.scryer.endpoint.configuration;

import com.scryer.endpoint.service.imagesrc.ImageSrc;
import com.scryer.endpoint.service.tag.TagModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfiguration {

    @Bean
    public ReactiveRedisTemplate<String, TagModel> tagRedisTemplate(final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext<String, TagModel> context = RedisSerializationContext
                .<String, TagModel>newSerializationContext().hashKey(new StringRedisSerializer())
                .hashValue(new Jackson2JsonRedisSerializer<>(TagModel.class)).key(new StringRedisSerializer())
                .value(new Jackson2JsonRedisSerializer<>(TagModel.class)).build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ImageSrc> imageRedisTemplate(final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext<String, ImageSrc> context = RedisSerializationContext
                .<String, ImageSrc>newSerializationContext().hashKey(new StringRedisSerializer())
                .hashValue(new Jackson2JsonRedisSerializer<>(ImageSrc.class)).key(new StringRedisSerializer())
                .value(new Jackson2JsonRedisSerializer<>(ImageSrc.class)).build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(@Qualifier("redisUri") final URI redisUri,
                                                                         @Qualifier("awsSecretAccessKey")
                                                                         final String awsSecretAccessKey) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisUri.getHost(),
                                                                                                     redisUri.getPort());
        redisStandaloneConfiguration.setPassword(awsSecretAccessKey);
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

}
