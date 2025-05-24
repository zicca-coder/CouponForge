package com.example.coupon.distribution.common.constant;

/**
 * 分发优惠券服务 RocketMQ 常量
 */
public class DistributionRocketMQConstant {

    /**
     * 优惠券模板推送执行 Topic Key
     * 负责扫描优惠券 Excel 并将里面的记录进行推送
     */
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "coupon_distribution-service_coupon-task-execute_topic-859608205";

    /**
     * 优惠券模板推送执行-执行消费者组 Key
     */
    public static final String TEMPLATE_TASK_EXECUTE_CG_KEY = "coupon_distribution-service_coupon-task-execute_cg-859608205";

    /**
     * 优惠券模板推送执行 Topic Key
     * 负责执行将优惠券发放给具体用户逻辑
     */
    public static final String TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY = "coupon_distribution-service_coupon-execute-distribution_topic-859608205";

    /**
     * 优惠券模板推送执行-执行消费者组 Key
     */
    public static final String TEMPLATE_EXECUTE_DISTRIBUTION_CG_KEY = "coupon_distribution-service_coupon-execute-distribution_cg-859608205";

    /**
     * 优惠券模板推送用户通知-执行消费者组 Key
     */
    public static final String TEMPLATE_EXECUTE_SEND_MESSAGE_CG_KEY = "coupon_distribution-service_coupon-execute-send-message_cg-859608205";
}
