package com.example.coupon.merchant.admin.common.constant;

/**
 * 商家后管优惠券模板 RocketMQ 常量
 */
public class MerchantAdminRocketMQConstant {

    /**
     * 优惠券关闭定时执行 Topic Key : 优惠券模板有效日期到后，修改优惠券模板状态为已停止
     */
    public static final String TEMPLATE_DELAY_TOPIC_KEY = "merchant-admin-service_coupon-template-delay_topic-859608205";
    /**
     * 优惠券关闭定时执行消费者组 Key
     */
    public static final String TEMPLATE_DELAY_STATUS_CG_KEY = "merchant-admin-service_coupon-template-delay-status_cg-859608205";


    /**
     * 优惠券模板推送执行 Topic Key -> distribution 模块中 CouponTaskExecuteConsumer 消费该消息
     * 负责扫描优惠券 Excel 并将里面的记录进行实际推送
     */
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "coupon_distribution-service_coupon-task-execute_topic-859608205";


    /**
     * 优惠券推送任务定时执行 Topic Key
     */
    public static final String TEMPLATE_TASK_DELAY_TOPIC_KEY = "coupon_merchant-admin-service_coupon-task-delay_topic-859608205";

    /**
     * 优惠券模板推送定时执行消费者组 key
     */
    public static final String TEMPLATE_TASK_DELAY_STATUS_CG_KEY = "coupon_merchant-admin-service_coupon-task-delay-status_cg-859608205";


}
