package com.example.coupon.merchant.admin.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDTO {
    // 用户id
    private String userId;
    // 用户名
    private String username;
    // 店铺id
    private Long shopNumber;
}
