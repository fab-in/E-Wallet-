package com.example.transaction_service.Config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.example.transaction_service.DTO.OtpData;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<UUID, OtpData> otpCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) 
                .maximumSize(10000) 
                .build();
    }
}

