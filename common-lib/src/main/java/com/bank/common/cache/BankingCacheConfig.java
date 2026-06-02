package com.bank.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared Redis cache config. Each cache name has its own TTL so we can tune
 * per-data-class (PII has shorter TTL, balance is shortest, lookup tables longest).
 *
 * Why per-cache TTLs:
 *  - 'accounts:by-id'  → short (5s) so balance views are nearly real-time
 *  - 'customers'       → 5 min (KYC/profile changes rare)
 *  - 'fraud:*'         → 1 min (velocity stats reset behavior)
 *  - 'transfers'       → 30s (status changes propagate quickly)
 */
@Configuration
public class BankingCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          ObjectMapper objectMapper) {
        var serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .disableCachingNullValues()
                .prefixCacheNameWith("bank::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("customers",                defaultCfg.entryTtl(Duration.ofMinutes(5)));
        perCache.put("customers:by-email",       defaultCfg.entryTtl(Duration.ofMinutes(5)));
        perCache.put("accounts:by-id",           defaultCfg.entryTtl(Duration.ofSeconds(5)));
        perCache.put("accounts:by-customer",     defaultCfg.entryTtl(Duration.ofSeconds(10)));
        perCache.put("transfers:by-id",          defaultCfg.entryTtl(Duration.ofSeconds(30)));
        perCache.put("fraud:daily-stats",        defaultCfg.entryTtl(Duration.ofMinutes(1)));
        perCache.put("fraud:known-counterparty", defaultCfg.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCfg)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }
}
