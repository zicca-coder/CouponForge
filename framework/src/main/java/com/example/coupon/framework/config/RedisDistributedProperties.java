package com.example.coupon.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式 Redis 缓存配置属性
 */
@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX) // Sping Boot 会查找配置文件中以framework.cache.redis开头的属性，并注入到该类中的属性中
public class RedisDistributedProperties {

    public static final String PREFIX = "framework.cache.redis";

    private String prefix;

    private String prefixCharset = "UTF-8";


}
