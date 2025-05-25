package com.example.coupon.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.example.coupon.engine.common.constant.EngineRocketMQConstant;
import com.example.coupon.engine.mq.base.BaseSendExtendDTO;
import com.example.coupon.engine.mq.base.MessageWrapper;
import com.example.coupon.engine.mq.event.UserCouponRedeemEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户优惠券兑换消息发送
 * 消费者：{@link com.example.coupon.engine.mq.consumer.UserCouponRedeemConsumer}
 */
@Component
public class UserCouponRedeemProducer extends AbstractCommonSendProduceTemplate<UserCouponRedeemEvent> {

    private final ConfigurableEnvironment environment;

    public UserCouponRedeemProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(UserCouponRedeemEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("用户兑换优惠券")
                .keys(UUID.randomUUID().toString())
                .topic(environment.resolvePlaceholders(EngineRocketMQConstant.COUPON_TEMPLATE_REDEEM_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(UserCouponRedeemEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
