package com.bank.common.config;

import com.bank.common.cache.BankingCacheConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
@Import(BankingCacheConfig.class)
public class CommonCacheAutoConfiguration {
}
