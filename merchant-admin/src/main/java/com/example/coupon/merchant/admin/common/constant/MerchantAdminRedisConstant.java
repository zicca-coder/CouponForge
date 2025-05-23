package com.example.coupon.merchant.admin.common.constant;

/**
 * 商家后管优惠券 Redis 常量类
 */
public class MerchantAdminRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "coupon:cache:template:exist:";

    /**
     * 布隆过滤器缓存统一前缀
     */
    public static final String COUPON_TEMPLATE_QUERY_BLOOM_FILTER_PREFIX = "coupon:filter:template:";

}
