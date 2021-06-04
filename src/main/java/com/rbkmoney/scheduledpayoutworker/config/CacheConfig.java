package com.rbkmoney.scheduledpayoutworker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.rbkmoney.scheduledpayoutworker.constant.CacheConstant.PARTIES;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(@Value("${cache.maxSize}") long cacheMaximumSize) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCacheNames(List.of(PARTIES));
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(cacheMaximumSize));
        return caffeineCacheManager;
    }

}
