package com.example.coupon.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.example.coupon.merchant.admin.common.constant.MerchantAdminRocketMQConstant;
import com.example.coupon.merchant.admin.mq.base.BaseSendExtendDTO;

import com.example.coupon.merchant.admin.mq.base.MessageWrapper;
import com.example.coupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 优惠券模板关闭定时执行生产者
 */
@Component
public class CouponTemplateDelayExecuteStatusProducer extends AbstractCommonSendProduceTemplate<CouponTemplateDelayEvent>{

    private final ConfigurableEnvironment environment;

    public CouponTemplateDelayExecuteStatusProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTemplateDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券模板定时关闭执行")
                .keys(String.valueOf(messageSendEvent.getCouponTemplateId()))
                .topic(environment.resolvePlaceholders(MerchantAdminRocketMQConstant.TEMPLATE_DELAY_TOPIC_KEY))
                .delayTime(messageSendEvent.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        /**
         * Message 类是 Spring Messaging 模块中的一个接口，用于封装消息数据。
         *      封装数据：Message 接口封装了一个 payload ，即消息的实际内容。这里的 payload 是一个 MessageWrapper 对象，
         *      携带元数据： Message 接口允许通过 header 来携带额外的元数据。 header 是一个键值对集合，可以用来存储如消息标识符、主题标签等信息。
         */
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
