package com.example.coupon.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CouponStatusEnum {

    /**
     * 生效中
     */
    EFFECTIVE(0),

    /**
     * 已结束
     */
    ENDED(1);

    @Getter
    private final int type;
}