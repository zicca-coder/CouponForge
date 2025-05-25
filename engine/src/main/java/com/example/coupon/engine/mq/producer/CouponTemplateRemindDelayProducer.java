package com.example.coupon.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.example.coupon.engine.common.constant.EngineRocketMQConstant;
import com.example.coupon.engine.mq.base.BaseSendExtendDTO;
import com.example.coupon.engine.mq.base.MessageWrapper;
import com.example.coupon.engine.mq.event.CouponTemplateRemindDelayEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 提醒抢券消息生产者
 * 消费者：{@link com.example.coupon.engine.mq.consumer.CouponTemplateRemindDelayConsumer}
 */
@Slf4j
@Component
public class CouponTemplateRemindDelayProducer extends AbstractCommonSendProduceTemplate<CouponTemplateRemindDelayEvent> {

    private final ConfigurableEnvironment environment;

    public CouponTemplateRemindDelayProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTemplateRemindDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("提醒用户抢券")
                .keys(messageSendEvent.getUserId() + ":" + messageSendEvent.getCouponTemplateId())
                .topic(environment.resolvePlaceholders(EngineRocketMQConstant.COUPON_TEMPLATE_REMIND_TOPIC_KEY))
                .sentTimeout(2000L)
                .delayTime(messageSendEvent.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateRemindDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
