package com.example.coupon.distribution.service.basics;

/**
 * 分发服务执行策略接口定义
 */
public interface DistributionExecuteStrategy<REQUEST, RESPONSE> {


    /**
     * 执行策略标识
     */
    default String mark() {
        return null;
    }

    /**
     * 执行策略
     * @param requestParam 执行策略入参
     */
    default void execute(REQUEST requestParam) {

    }

    /**
     *  执行策略，带返回值
     * @param requestParam 执行策略入参
     */
    default RESPONSE executeResp(REQUEST requestParam) {
        return null;
    }


}
