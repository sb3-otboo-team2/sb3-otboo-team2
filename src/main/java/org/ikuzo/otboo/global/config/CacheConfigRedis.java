package org.ikuzo.otboo.global.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@EnableCaching
@Profile("prod")
public class CacheConfigRedis {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
        @Value("${spring.data.redis.host}") String host,
        @Value("${spring.data.redis.port}") int port,
        @Value("${spring.data.redis.password:}") String password,
        @Value("${spring.data.redis.ssl.enabled:false}") boolean ssl) {

        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            conf.setPassword(password);
        }
        LettuceClientConfiguration.LettuceClientConfigurationBuilder b = LettuceClientConfiguration.builder();
        if (ssl) {
            b.useSsl();
        }
        b.commandTimeout(Duration.ofSeconds(3));
        return new LettuceConnectionFactory(conf, b.build());
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        log.info("Redis cache manager is enabled.");
        // 기본 직렬화(Jackson), TTL 등 공통 정책
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));

        // 캐시별 세부 TTL (기존 Caffeine 설정 이름을 그대로 유지)
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("notifications", defaults.entryTtl(Duration.ofSeconds(60)));
        perCache.put("followers", defaults.entryTtl(Duration.ofSeconds(600)));
        perCache.put("followings", defaults.entryTtl(Duration.ofSeconds(600)));
        perCache.put("followSummary", defaults.entryTtl(Duration.ofSeconds(300)));
        perCache.put("weatherByCoordinates", defaults.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(cf)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(perCache)
            .transactionAware()
            .build();
    }
}

