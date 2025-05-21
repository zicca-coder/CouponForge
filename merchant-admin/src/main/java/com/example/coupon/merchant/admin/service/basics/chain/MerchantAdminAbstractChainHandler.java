package com.example.coupon.merchant.admin.service.basics.chain;

import org.springframework.core.Ordered;

/**
 * 商家后管业务责任链接口
 * 实现 Ordered 接口，保证执行顺序 -> 责任链中会有多个处理器，需要顺序执行，优先处理性能较好的，如果不满足可以直接返回，而不需要后续验证
 * @param <T>
 */
public interface MerchantAdminAbstractChainHandler<T> extends Ordered {

    /**
     * 执行责任链逻辑
     * @param requestParam 请求参数
     */
    void handler(T requestParam);

    /**
     * 责任链组件标识
     */
    String mark();
}
