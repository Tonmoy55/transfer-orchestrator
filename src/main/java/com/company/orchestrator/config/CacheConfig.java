package com.company.orchestrator.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for policy caching.
 *
 * Note: Spring Boot's cache abstraction is configured to use a simple
 * in-memory cache here. If you want to switch to the dedicated
 * spring-cache-caffeine module later, you can replace the
 * CacheManager implementation with CaffeineCacheManager without
 * touching the rest of the code.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACTIVE_POLICIES_CACHE = "activePolicies";

    @Bean
    public CacheManager cacheManager() {
        // For now use a simple ConcurrentMap-based cache. Spring Boot 4's
        // cache abstraction makes it easy to swap this for Caffeine later.
        return new ConcurrentMapCacheManager(ACTIVE_POLICIES_CACHE);
    }
}
