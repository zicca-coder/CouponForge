package com.example.coupon.engine.config;

import com.example.coupon.engine.common.constant.EngineRedisConstant;
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
    public RBloomFilter<String> couponTemplateBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(EngineRedisConstant.COUPON_TEMPLATE_QUERY_BLOOM_FILTER_PREFIX + "couponTemplateQueryBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> cancelRemindBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(EngineRedisConstant.COUPON_CANCEL_REMIND_BLOOM_FILTER_PREFIX + "cancelRemindBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

}
