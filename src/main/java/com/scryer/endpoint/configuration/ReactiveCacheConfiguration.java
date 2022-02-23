package com.scryer.endpoint.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class ReactiveCacheConfiguration {

//    @Bean(initMethod = "start", destroyMethod = "stop")
//    public RedisServer redisServer() throws IOException {
//        return new RedisServer(6379);
//    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("logout");
}

    @Bean
    ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory();
    }
}
