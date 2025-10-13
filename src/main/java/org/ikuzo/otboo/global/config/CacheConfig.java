package org.ikuzo.otboo.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                     ObjectMapper objectMapper) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        CaffeineCache notifications = new CaffeineCache("notifications",
            Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.SECONDS)  // 알림: 1분
                .maximumSize(100)
                .recordStats()
                .build());

        CaffeineCache followers = new CaffeineCache("followers",
            Caffeine.newBuilder()
                .expireAfterAccess(600, TimeUnit.SECONDS) // 팔로워: 10분
                .maximumSize(1000)
                .recordStats()
                .build());

        CaffeineCache followings = new CaffeineCache("followings",
            Caffeine.newBuilder()
                .expireAfterAccess(600, TimeUnit.SECONDS) // 팔로잉: 10분
                .maximumSize(1000)
                .recordStats()
                .build());

        CaffeineCache followSummary = new CaffeineCache("followSummary",
            Caffeine.newBuilder()
                .expireAfterAccess(300, TimeUnit.SECONDS) // 요약: 5분
                .maximumSize(1000)
                .recordStats()
                .build());

        cacheManager.setCaches(Arrays.asList(
            notifications, followers, followings, followSummary
        ));
        cacheManager.initializeCaches();

        RedisCacheConfiguration redisDefaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> redisCaches = new HashMap<>();
        redisCaches.put("weatherByCoordinates", redisDefaultConfig.entryTtl(Duration.ofMinutes(30)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisDefaultConfig)
            .withInitialCacheConfigurations(redisCaches)
            .build();

        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(redisCacheManager, cacheManager);
        compositeCacheManager.setFallbackToNoOpCache(false);
        return compositeCacheManager;
    }
}
