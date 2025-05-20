package com.example.coupon.merchant.admin.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 用户登录信息存储上下文
 */
public class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_INFO_LOCAL = new TransmittableThreadLocal<>();

    public static void setUser(UserInfoDTO user) {
        USER_INFO_LOCAL.set(user);
    }

    public static String getUserId() {
        UserInfoDTO userInfoDTO = USER_INFO_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    public static String getUsername() {
        UserInfoDTO userInfoDTO = USER_INFO_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    public static Long getShopNumber() {
        UserInfoDTO userInfoDTO = USER_INFO_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getShopNumber).orElse(null);
    }

    public static void removeUser() {
        USER_INFO_LOCAL.remove();
    }
}
