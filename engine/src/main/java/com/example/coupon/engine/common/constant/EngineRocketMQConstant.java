package com.example.coupon.engine.common.constant;

public class EngineRocketMQConstant {
    /**
     * 用户优惠券到期后关闭 Topic Key
     */
    public static final String USER_COUPON_DELAY_CLOSE_TOPIC_KEY = "one-coupon_engine-service_user-coupon-delay-close_topic-859608205";

    /**
     * 用户优惠券到期后关闭消费者组 Key
     */
    public static final String USER_COUPON_DELAY_CLOSE_CG_KEY = "one-coupon_engine-service_user-coupon-delay-close_cg-859608205";


    /**
     * 用户兑换优惠券 Topic Key
     */
    public static final String COUPON_TEMPLATE_REDEEM_TOPIC_KEY = "one-coupon_engine-service_coupon-redeem_topic-859608205";

    /**
     * 用户兑换优惠券消费者组 Key
     */
    public static final String COUPON_TEMPLATE_REDEEM_CG_KEY = "one-coupon_engine-service_coupon-redeem_cg-859608205";

}
