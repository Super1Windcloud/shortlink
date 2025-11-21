package org.superwindcloud.shortlink.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String ORIGINAL_URL_CACHE = "shortLinksByOriginalUrl";
    public static final String SHORT_CODE_CACHE = "shortLinksByShortCode";

    @Bean
    public Caffeine<Object, Object> caffeine() {
        return Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats();
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ORIGINAL_URL_CACHE, SHORT_CODE_CACHE);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
