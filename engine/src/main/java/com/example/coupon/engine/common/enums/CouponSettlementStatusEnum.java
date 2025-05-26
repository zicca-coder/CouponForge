package com.example.coupon.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券结算状态枚举
 */
@RequiredArgsConstructor
public enum CouponSettlementStatusEnum {

    /**
     * 锁定状态
     */
    LOCKED(0),

    /**
     * 已取消
     */
    CANCELED(1),

    /**
     * 已支付
     */
    PAID(2),

    /**
     * 已退款
     */
    REFUNDED(3);



    @Getter
    private final int status;
}
