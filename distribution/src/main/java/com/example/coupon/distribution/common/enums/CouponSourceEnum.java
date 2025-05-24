package com.example.coupon.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券来源枚举
 */
@RequiredArgsConstructor
public enum CouponSourceEnum {

    /**
     * 店铺券
     */
    SHOP(0),

    /**
     * 平台券
     */
    PLATFORM(1);

    @Getter
    private final int type;
}
