package com.example.coupon.engine.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.coupon.engine.common.constant.EngineRedisConstant;
import com.example.coupon.engine.common.constant.EngineRocketMQConstant;
import com.example.coupon.engine.common.context.UserContext;
import com.example.coupon.engine.common.enums.UserCouponStatusEnum;
import com.example.coupon.engine.dao.entity.UserCouponDO;
import com.example.coupon.engine.dao.mapper.UserCouponMapper;
import com.example.coupon.engine.mq.base.MessageWrapper;
import com.example.coupon.engine.mq.event.UserCouponDelayCloseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户优惠券延迟关闭消费者 -> 用户领取的优惠券有效过后，将该 用户-优惠券 记录从缓存中删除，同时修改数据库中该用户领取的优惠券记录状态为已过期
 * 生产者： {@link com.example.coupon.engine.mq.producer.UserCouponDelayCloseProducer}
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRocketMQConstant.USER_COUPON_DELAY_CLOSE_TOPIC_KEY,
        consumerGroup = EngineRocketMQConstant.USER_COUPON_DELAY_CLOSE_CG_KEY
)
@Slf4j(topic = "UserCouponDelayCloseConsumer")
public class UserCouponDelayCloseConsumer implements RocketMQListener<MessageWrapper<UserCouponDelayCloseEvent>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserCouponMapper userCouponMapper;

    @Override
    public void onMessage(MessageWrapper<UserCouponDelayCloseEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 延迟关闭用户已领取优惠券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));
        UserCouponDelayCloseEvent event = messageWrapper.getMessage();

        // 删除用户领取优惠券模板缓存记录
        String userCouponListCacheKey = EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY + UserContext.getUserId();
        String userCouponItemCacheKey = StrUtil.builder()
                .append(event.getCouponTemplateId())
                .append("_")
                .append(event.getUserCouponId())
                .toString();
        Long removed = stringRedisTemplate.opsForZSet().remove(userCouponListCacheKey, userCouponItemCacheKey);
        if (removed == null || removed == 0L) {
            return;
        }

        // 修改用户领券记录状态为已过期
        UserCouponDO userCouponDO = UserCouponDO.builder()
                .status(UserCouponStatusEnum.EXPIRED.getCode())
                .build();
        LambdaUpdateWrapper<UserCouponDO> updateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                .eq(UserCouponDO::getId, event.getUserCouponId())
                .eq(UserCouponDO::getUserId, event.getUserId())
                .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode())
                .eq(UserCouponDO::getCouponTemplateId, event.getCouponTemplateId());
        userCouponMapper.update(userCouponDO, updateWrapper);
    }
}
