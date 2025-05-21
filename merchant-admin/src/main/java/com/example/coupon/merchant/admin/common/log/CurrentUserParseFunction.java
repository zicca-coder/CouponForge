package com.example.coupon.merchant.admin.common.log;

import com.example.coupon.merchant.admin.common.context.UserContext;
import com.mzt.logapi.service.IParseFunction;
import org.springframework.stereotype.Component;

/**
 * 操作日志组件解析当前登录用户信息
 */
@Component
public class CurrentUserParseFunction implements IParseFunction {
    @Override
    public String functionName() {
        return "CURRENT_USER";
    }

    @Override
    public String apply(Object value) {
        return UserContext.getUsername();
    }
}
