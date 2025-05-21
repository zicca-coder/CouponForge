package com.example.coupon.merchant.admin.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.coupon.merchant.admin.common.constant.MerchantAdminRocketMQConstant;
import com.example.coupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.example.coupon.merchant.admin.mq.base.MessageWrapper;
import com.example.coupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import com.example.coupon.merchant.admin.service.impl.CouponTemplateServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券模板关闭定时任务消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MerchantAdminRocketMQConstant.TEMPLATE_DELAY_TOPIC_KEY,
        consumerGroup = MerchantAdminRocketMQConstant.TEMPLATE_DELAY_STATUS_CG_KEY
)
@Slf4j(topic = "CouponTemplateDelayExecuteStatusConsumer")
public class CouponTemplateDelayExecuteStatusConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDelayEvent>> {

    private final CouponTemplateServiceImpl couponTemplateService;

    @Override
    public void onMessage(MessageWrapper<CouponTemplateDelayEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 优惠券模板定时执行@变更模板表状态 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        // 修改指定优惠券模板状态为已结束
        CouponTemplateDelayEvent message = messageWrapper.getMessage();
        LambdaUpdateWrapper updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, message.getCouponTemplateId())
                .eq(CouponTemplateDO::getShopNumber, message.getShopNumber());
        CouponTemplateDO couponTemplateDO = CouponTemplateDO.builder().status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build();
        couponTemplateService.update(couponTemplateDO, updateWrapper);
    }
}
