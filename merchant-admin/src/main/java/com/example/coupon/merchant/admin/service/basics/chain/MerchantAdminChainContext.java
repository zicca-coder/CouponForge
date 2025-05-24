package com.example.coupon.merchant.admin.service.basics.chain;

import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 商家后管模块责任链上下文容器
 * @param <T>
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    /**
     * Spring 上下文，用于获取所有 {@link MerchantAdminAbstractChainHandler} 组件
     */
    private ApplicationContext applicationContext;

    /**
     * 商家后管模块责任链组件容器 -> 这里体现了享元模式，通过共享同一个实例，减少内存消耗
     * <p>
     *     Key: 责任链标识 {@link MerchantAdminAbstractChainHandler#mark()}
     *     Val: 责任链的各个组件的集合 {@link List<MerchantAdminAbstractChainHandler>}
     * </p>
     * <p>
     *     以优惠券模板创建逻辑责任链为例，实例如下：
     *     Key: MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY
     *     Val: [CouponTemplateCreateParamNotNullChainFilter、CouponTemplateCreateParamBaseVerifyChainFilter、MerchantAdminAbstractChainHandler]
     *     - {@link com.example.coupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamNotNullChainFilter}
     *     - {@link com.example.coupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamBaseVerifyChainFilter}
     *     - {@link com.example.coupon.merchant.admin.service.handler.filter.CouponTemplateCreateParamVerifyChainFilter}
     * </p>
     */
    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 责任链组件执行
     *
     * @param mark 责任链标识
     * @param requestParam 请求参数
     */
    public void handler(String mark, T requestParam) {
        // 根据 mark 标识从责任链容器中获取一组责任链实现 Bean 集合
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlerContainer)) {
            throw new RuntimeException(String.format("[%s] Chain of Reponsibility ID is undefined.",  mark));
        }
        // 遍历责任链集合，依次执行 handler 方法
        abstractChainHandlers.forEach(handler -> handler.handler(requestParam));
    }

    /**
     * CommandLineRunner 接口，用于在项目启动时执行
     * @param args incoming main method arguments
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            // 判断 Mark 是否已经存在责任链容器中，如果存在返回集合；如果不存在，创建 Mark 和对应的集合，并返回集合
            List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.getOrDefault(bean.mark(), new ArrayList<>()); // getOrDefault: 根据 Key 返回一个Value, 如果不存在则返回一个默认值
            // 将 Bean 添加到集合中
            abstractChainHandlers.add(bean);
            // 将新集合重写到责任链容器中
            abstractChainHandlerContainer.put(bean.mark(), abstractChainHandlers);
        });
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) -> {
            // 对责任链容器中都的每个集合进行排序
            unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder));
        });
    }

    /**
     * ApplicationContextAware 的实现方法，用于获取 Spring 上下文
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
