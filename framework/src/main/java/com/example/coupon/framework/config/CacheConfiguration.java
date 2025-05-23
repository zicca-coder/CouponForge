package com.example.coupon.framework.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

/**
 * 分布式 Redis 缓存配置
 */
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisDistributedProperties.class)
public class CacheConfiguration implements InitializingBean {

    private final RedisDistributedProperties redisDistributedProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Bean
    public RedisKeySerializer redisKeySerializer() {
        String prefix = Optional.ofNullable(redisDistributedProperties.getPrefix()).orElse("");
        String prefixCharset = redisDistributedProperties.getPrefixCharset();
        return new RedisKeySerializer(prefix, prefixCharset);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        stringRedisTemplate.setKeySerializer(redisKeySerializer());
    }
}
