package com.example.coupon.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券类型枚举
 */
@RequiredArgsConstructor
public enum DiscountTypeEnum {
    /**
     * 立减券
     */
    FIXED_DISCOUNT(0, "立减券"),

    /**
     * 满减券
     */
    THRESHOLD_DISCOUNT(1, "满减券"),

    /**
     * 折扣券
     */
    DISCOUNT_COUPON(2, "折扣券");

    @Getter
    private final int type;

    @Getter
    private final String value;

    /**
     * 根据 type 找到对应的 value
     *
     * @param type 要查找的类型代码
     * @return 对应的描述值，如果没有找到抛异常
     */
    public static String findValueByType(int type) {
        for (DiscountTypeEnum target : DiscountTypeEnum.values()) {
            if (target.getType() == type) {
                return target.getValue();
            }
        }
        throw new IllegalArgumentException();
    }
}
