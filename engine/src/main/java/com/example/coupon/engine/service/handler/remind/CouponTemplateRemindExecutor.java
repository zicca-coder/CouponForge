package com.example.coupon.engine.service.handler.remind;


import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.example.coupon.engine.common.constant.EngineRedisConstant;
import com.example.coupon.engine.common.enums.CouponRemindTypeEnum;
import com.example.coupon.engine.mq.event.CouponTemplateRemindDelayEvent;
import com.example.coupon.engine.mq.producer.CouponTemplateRemindDelayProducer;
import com.example.coupon.engine.service.CouponTemplateRemindService;
import com.example.coupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import com.example.coupon.engine.service.handler.remind.impl.SendAppMessageRemindCouponTemplate;
import com.example.coupon.engine.service.handler.remind.impl.SendEmailRemindCouponTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * 执行响应的抢券提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTemplateRemindExecutor {

    private final CouponTemplateRemindService couponTemplateRemindService;
    private final SendEmailRemindCouponTemplate sendEmailRemindCouponTemplate;
    private final SendAppMessageRemindCouponTemplate sendAppMessageRemindCouponTemplate;

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    // 提醒用户属于 IO 密集型任务
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() << 1,
            Runtime.getRuntime().availableProcessors() << 2,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    public static final String REDIS_BLOCKING_DEQUE = "COUPON_REMIND_QUEUE";

    /**
     * 执行提醒
     *
     * @param couponTemplateRemindDTO 用户预约提醒请求信息
     */
    public void executeRemindCouponTemplate(CouponTemplateRemindDTO couponTemplateRemindDTO) {
        // 用户没取消预约，则发出提醒
        if (couponTemplateRemindService.isCancelRemind(couponTemplateRemindDTO)) {
            log.info("用户已取消优惠券预约提醒，参数：{}", JSON.toJSONString(couponTemplateRemindDTO));
            return;
        }

        // 假设刚把消息提交到线程池，突然应用宕机了，我们通过延迟队列进行兜底 Refresh
        RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(REDIS_BLOCKING_DEQUE);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        String key = EngineRedisConstant.COUPON_REMIND_CHECK_KEY + couponTemplateRemindDTO.getUserId() + "_" + couponTemplateRemindDTO.getCouponTemplateId() + "_" + couponTemplateRemindDTO.getRemindTime() + "_" + couponTemplateRemindDTO.getType();
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(couponTemplateRemindDTO));
        delayedQueue.offer(key, 10, TimeUnit.SECONDS);

        /**
         * todo: 消费者不是多线程的吗？
         * 为了避免消息队列的消费速度慢，采用线程池进行并行发送，以提高消息处理和发送的效率
         */
        executorService.execute(() -> {
            // 向用户发起消息提醒
            switch (Objects.requireNonNull(CouponRemindTypeEnum.getByType(couponTemplateRemindDTO.getType()))) {
                case APP -> sendAppMessageRemindCouponTemplate.remind(couponTemplateRemindDTO);
                case EMAIL -> sendEmailRemindCouponTemplate.remind(couponTemplateRemindDTO);
                default -> {
                }
            }

            // 提醒用户后删除 Key
            stringRedisTemplate.delete(key);
        });
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class RefreshCouponRemindDelayQueueRunner implements CommandLineRunner {

        private final CouponTemplateRemindDelayProducer couponTemplateRemindDelayProducer;
        private final RedissonClient redissonClient;
        private final StringRedisTemplate stringRedisTemplate;

        @Override
        public void run(String... args) {
            Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread = new Thread(runnable);
                                thread.setName("delay_coupon-remind_consumer");
                                thread.setDaemon(Boolean.TRUE);
                                return thread;
                            })
                    .execute(() -> {
                        RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(REDIS_BLOCKING_DEQUE);
                        for (; ; ) {
                            try {
                                // 获取延迟队列待消费 Key
                                String key = blockingDeque.take();
                                if (stringRedisTemplate.hasKey(key)) {
                                    log.info("检查用户发送的通知消息Key：{} 未消费完成，开启重新投递", key);

                                    // Redis 中还存在该 Key，说明任务没被消费完，则可能是消费机器宕机了，重新投递消息
                                    CouponTemplateRemindDelayEvent couponRemindEvent = JSONUtil.toBean(stringRedisTemplate.opsForValue().get(key), CouponTemplateRemindDelayEvent.class);
                                    couponTemplateRemindDelayProducer.sendMessage(couponRemindEvent);

                                    // 提醒用户后删除 Key
                                    stringRedisTemplate.delete(key);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        }
    }


}
