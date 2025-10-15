package org.ikuzo.otboo.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
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

        CaffeineCache clothes = new CaffeineCache("clothes",
            Caffeine.newBuilder()
                .expireAfterAccess(600, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()
                .build());

        CaffeineCache clothesAttributeDef = new CaffeineCache("clothesAttributeDef",
            Caffeine.newBuilder()
                .expireAfterAccess(3600, TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats()
                .build());

        CaffeineCache weatherByCoordinates = new CaffeineCache("weatherByCoordinates",
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build());

        cacheManager.setCaches(List.of(
            notifications,
            followers,
            followings,
            followSummary,
            clothes,
            clothesAttributeDef,
            weatherByCoordinates
        ));
        cacheManager.initializeCaches();
        return cacheManager;
    }
}
