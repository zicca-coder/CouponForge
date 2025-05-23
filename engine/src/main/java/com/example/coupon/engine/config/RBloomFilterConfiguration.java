package com.example.coupon.engine.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置类
 */
@Configuration
public class RBloomFilterConfiguration {

    @Bean
    public RBloomFilter<String> couponTemplateBloomFilter(RedissonClient redissonClient, @Value("${framework.cache.redis.prefix:}") String cachePrefix) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(String.format(cachePrefix, "couponTemplateBloomFilter"));
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> cancelRemindBloomFilter(RedissonClient redissonClient, @Value("${framework.cache.redis.prefix:}") String cachePrefix) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(String.format(cachePrefix, "cancelRemindBloomFilter"));
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

}
