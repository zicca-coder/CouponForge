package com.example.coupon.merchant.admin.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券模板定时执行事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTemplateDelayEvent {
    /**
     * 店铺id
     */
    private Long shopNumber;

    /**
     * 优惠券模板id
     */
    private Long couponTemplateId;

    /**
     * 具体延迟时间
     */
    private Long delayTime;
}
