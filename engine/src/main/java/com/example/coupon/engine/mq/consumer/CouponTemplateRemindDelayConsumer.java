package com.example.coupon.engine.mq.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.example.coupon.engine.common.constant.EngineRocketMQConstant;
import com.example.coupon.engine.mq.base.MessageWrapper;
import com.example.coupon.engine.mq.event.CouponTemplateRemindDelayEvent;
import com.example.coupon.engine.service.handler.remind.CouponTemplateRemindExecutor;
import com.example.coupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 提醒用户抢券消费者
 * 生产者：{@link com.example.coupon.engine.mq.producer.CouponTemplateRemindDelayProducer}
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRocketMQConstant.COUPON_TEMPLATE_REMIND_TOPIC_KEY,
        consumerGroup = EngineRocketMQConstant.COUPON_TEMPLATE_REMIND_CG_KEY
)
@Slf4j(topic = "CouponTemplateRemindDelayConsumer")
public class CouponTemplateRemindDelayConsumer implements RocketMQListener<MessageWrapper<CouponTemplateRemindDelayEvent>> {

    private final CouponTemplateRemindExecutor couponTemplateRemindExecutor;

    /**
     * 默认机制：RocketMQ 消费者默认使用单线程拉取并消费消息，处理逻辑是串行的
     * 同步处理问题：若直接在 onMessage 中执行耗时操作，会阻塞消费者线程，导致消息堆积
     * 此处推送消息属性网络IO操作，会耗时，因此采用线程池异步处理，提高处理效率
     *
     * <p>
     *     核心线程池的作用：
     *     1. 解耦消息拉取与业务处理
     *          消费者线程职责：仅负责从 Broker 拉取消息并提交给线程池
     *          业务线程职责：由独立线程处理具体业务逻辑（如发送短信，更新 DB 等）
     *     2. 提升吞吐量
     *          并行消费：允许消费者线程快速响应 Broker ，持续拉取消息
     *          资源隔离：避免业务异常（如超时）影响整个消费者状态
     *     3. 若开启 RocketMQ 的消费者并行消费，仍建议使用线程池：
     *          RocketMQ 线程池用于控制消费并发度
     *          业务线程池用于隔离耗时操作，防止阻塞消费线程
     * </p>
     * @param messageWrapper 消息包装类
     */
    @Override
    public void onMessage(MessageWrapper<CouponTemplateRemindDelayEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 提醒用户抢券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateRemindDelayEvent event = messageWrapper.getMessage();
        CouponTemplateRemindDTO couponTemplateRemindDTO = BeanUtil.toBean(event, CouponTemplateRemindDTO.class);


        // 根据不同策略向用户发送消息提醒
        couponTemplateRemindExecutor.executeRemindCouponTemplate(couponTemplateRemindDTO);
    }
}
