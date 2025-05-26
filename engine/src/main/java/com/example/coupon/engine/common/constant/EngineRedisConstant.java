package com.example.coupon.engine.common.constant;

/**
 * 分布式 Redis 缓存引擎层常量类
 */
public class EngineRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "coupon:cache:template:exist:";


    /**
     * 优惠券模板缓存空值 Key
     *      用于解决缓存穿透问题，当查询数据库中没有该优惠券模板后，将请求中传来的 id 缓存到 redis （COUPON_TEMPLATE_NOT_EXIST） 中
     *      当下次请求再来时，如果缓存中有该id，证明不存在，返回空值
     */
    public static final String COUPON_TEMPLATE_NOT_EXIST = "coupon:cache:template:not_exist:";


    /**
     * 优惠券模板缓存分布式锁 Key
     */
    public static final String COUPON_TEMPLATE_LOCK_KEY = "coupon:lock:template:";


    /**
     * 布隆过滤器缓存统一前缀
     */
    public static final String COUPON_TEMPLATE_QUERY_BLOOM_FILTER_PREFIX = "coupon:filter:template:";

    public static final String COUPON_CANCEL_REMIND_BLOOM_FILTER_PREFIX = "coupon:filter:remind:";


    /**
     * 用户已领取优惠券列表模板 Key
     */
    public static final String USER_COUPON_TEMPLATE_LIST_KEY = "coupon:cache:engine:user-template-list:";

    /**
     * 限制用户领取优惠券模板次数缓存 Key
     */
    public static final String USER_COUPON_TEMPLATE_LIMIT_KEY = "coupon:cache:engine:user-template-limit:";


    /**
     * 检查用户是否已提醒 Key
     */
    public static final String COUPON_REMIND_CHECK_KEY = "coupon:cache:engine:coupon-remind-check:";


    /**
     * 用户预约提醒信息 Key
     */
    public static final String USER_COUPON_TEMPLATE_REMIND_INFORMATION = "coupon:cache:engine:coupon-remind-information:";



    /**
     * 优惠券结算单分布式锁 Key
     */
    public static final String LOCK_COUPON_SETTLEMENT_KEY = "coupon:lock:engine:coupon-settlement:";


}
