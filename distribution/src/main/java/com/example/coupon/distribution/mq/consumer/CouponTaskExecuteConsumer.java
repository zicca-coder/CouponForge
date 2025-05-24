package com.example.coupon.distribution.mq.consumer;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.coupon.distribution.common.constant.DistributionRocketMQConstant;
import com.example.coupon.distribution.common.enums.CouponTaskStatusEnum;
import com.example.coupon.distribution.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.distribution.dao.entity.CouponTaskDO;
import com.example.coupon.distribution.dao.entity.CouponTemplateDO;
import com.example.coupon.distribution.dao.mapper.CouponTaskFailMapper;
import com.example.coupon.distribution.dao.mapper.CouponTaskMapper;
import com.example.coupon.distribution.dao.mapper.CouponTemplateMapper;
import com.example.coupon.distribution.mq.base.MessageWrapper;
import com.example.coupon.distribution.mq.event.CouponTaskExecuteEvent;
import com.example.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.example.coupon.distribution.service.handler.excel.CouponTaskExcelObject;
import com.example.coupon.distribution.service.handler.excel.ReadExcelDistributionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.sql.Wrapper;

/**
 * 优惠券推送任务定时执行 - 真实执行消费者
 * 从 Excel 中读取用户信息，并推送优惠券
 * 消息生产者：{@link com.example.coupon.merchant.admin.mq.producer.CouponTaskActualExecuteProducer}
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = DistributionRocketMQConstant.TEMPLATE_TASK_EXECUTE_TOPIC_KEY,
        consumerGroup = DistributionRocketMQConstant.TEMPLATE_TASK_EXECUTE_TOPIC_KEY
)
@Slf4j(topic = "CouponTaskExecuteConsumer")
public class CouponTaskExecuteConsumer implements RocketMQListener<MessageWrapper<CouponTaskExecuteEvent>> {

    private final CouponTaskMapper couponTaskMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponTaskFailMapper couponTaskFailMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponExecuteDistributionProducer couponExecuteDistributionProducer;


    @Override
    public void onMessage(MessageWrapper<CouponTaskExecuteEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 优惠券推送任务正式执行 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        Long couponTaskId = messageWrapper.getMessage().getCouponTaskId();
        CouponTaskDO couponTaskDO = couponTaskMapper.selectById(couponTaskId);
        // 判断【优惠券模板推送任务】是否是执行中，如果未到推送时间商家将任务推送活动取消了，则无需再执行推送任务了
        if (ObjectUtil.notEqual(CouponTaskStatusEnum.IN_PROGRESS.getStatus(), couponTaskDO.getStatus())) {
            log.warn("[消费者] 优惠券推送任务正式执行 - 推送任务记录状态异常：{}，已终止推送", couponTaskDO.getStatus());
            return;
        }

        // 再次判断优惠券模板的状态是否有效，如果商家创建了优惠券推送任务，但后面又结束了优惠券模板，则推送活动不应再进行
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class).eq(CouponTemplateDO::getId, couponTaskDO.getCouponTemplateId())
                .eq(CouponTemplateDO::getShopNumber, couponTaskDO.getShopNumber());
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        if (ObjectUtil.notEqual(CouponTemplateStatusEnum.ACTIVE.getStatus(), couponTemplateDO.getStatus())) {
            log.error("[消费者] 优惠券推送任务正式执行 - 优惠券ID：{}，优惠券模板状态：{}", couponTaskDO.getCouponTemplateId(), couponTemplateDO.getStatus());
            return;
        }


        // 正式开始执行优惠券推送任务
        // 从 Excel 表中读取到用户数据，进行推送
        ReadExcelDistributionListener readExcelDistributionListener = new ReadExcelDistributionListener(
                couponTaskDO,
                couponTemplateDO,
                couponTaskFailMapper,
                stringRedisTemplate,
                couponExecuteDistributionProducer
        );

        // todo: 这里还需改进，用户创建推送任务后，我们需要将 Excel 上传保存到服务器，然后保存该地址到数据库中，然后读取该地址进行推送
        // todo: 这里为实现上传功能，仅保存在本地，实际开发中请上传到服务器
        EasyExcel.read(couponTaskDO.getFileAddress(), CouponTaskExcelObject.class,  readExcelDistributionListener).sheet().doRead();

    }
}
