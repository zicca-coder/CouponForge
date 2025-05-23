package com.example.coupon.framework.config;

import com.example.coupon.framework.idempotent.NoDuplicateSubmitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等组件相关配置类
 */
@Configuration
public class IdempotentConfiguration {

    /**
     * 防止用户重复提交表达信息切面控制器
     */
    @Bean
    public NoDuplicateSubmitAspect noDuplicateSubmitAspect(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {
        return new NoDuplicateSubmitAspect(redissonClient, redisTemplate);
    }


}
