package com.example.coupon.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.example.coupon.distribution.common.constant.DistributionRedisConstant;
import com.example.coupon.distribution.common.constant.DistributionRocketMQConstant;
import com.example.coupon.distribution.common.constant.EngineRedisConstant;
import com.example.coupon.distribution.common.enums.CouponSourceEnum;
import com.example.coupon.distribution.common.enums.CouponStatusEnum;
import com.example.coupon.distribution.common.enums.CouponTaskStatusEnum;
import com.example.coupon.distribution.dao.entity.CouponTaskDO;
import com.example.coupon.distribution.dao.entity.CouponTaskFailDO;
import com.example.coupon.distribution.dao.entity.CouponTemplateDO;
import com.example.coupon.distribution.dao.entity.UserCouponDO;
import com.example.coupon.distribution.dao.mapper.CouponTaskFailMapper;
import com.example.coupon.distribution.dao.mapper.CouponTaskMapper;
import com.example.coupon.distribution.dao.mapper.CouponTemplateMapper;
import com.example.coupon.distribution.dao.mapper.UserCouponMapper;
import com.example.coupon.distribution.mq.base.MessageWrapper;
import com.example.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.example.coupon.distribution.service.handler.excel.UserCouponTaskFailExcelObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchExecutorException;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.sql.Wrapper;
import java.util.*;

/**
 * 优惠券执行分发到用户消费者
 * 生产者： {@link com.example.coupon.distribution.mq.producer.CouponExecuteDistributionProducer}
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY,
        consumerGroup = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_CG_KEY
)
@Slf4j(topic = "CouponExecuteDistributionConsumer")
public class CouponExecuteDistributionConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDistributionEvent>> {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponTaskMapper couponTaskMapper;
    private final CouponTaskFailMapper couponTaskFailMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Autowired
    private CouponExecuteDistributionConsumer couponExecuteDistributionConsumer;

    private final static int BATCH_USER_COUPON_SIZE = 50;
    private static final String BATCH_SAVE_USER_COUPON_LUA_PATH = "lua/batch_user_coupon_list.lua";
    private final String excelPath = "E:/excel";


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<CouponTemplateDistributionEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 优惠券任务执行推送@分发到用户账号 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        // 当保存用户优惠券集合达到批量保存数量
        CouponTemplateDistributionEvent event = messageWrapper.getMessage();
        if (!event.getDistributionEndFlag() && event.getBatchUserSetSize() % BATCH_USER_COUPON_SIZE == 0) {
            decrementCouponTemplateStockAndSaveUserCouponList(event);
        }
        // 分发任务结束标识为 True, 代码已经没有 Excel 记录了
        if (event.getDistributionEndFlag()) {
            String batchUserSetKey = DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY + event.getCouponTaskId();
            Long batchUserIdsSize = stringRedisTemplate.opsForSet().size(batchUserSetKey);
            event.setBatchUserSetSize(batchUserIdsSize.intValue());

            decrementCouponTemplateStockAndSaveUserCouponList(event);
            List<String> batchUserMaps = stringRedisTemplate.opsForSet().pop(batchUserSetKey, Integer.MAX_VALUE);
            // 此时待保存入库用户优惠券列表如果还有值，就意味着可能是库存不足引起的
            if (CollUtil.isNotEmpty(batchUserMaps)) {
                // 添加到 t_coupon_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
                List<CouponTaskFailDO> couponTaskFailDOList = new ArrayList<>(batchUserMaps.size());
                for (String batchUserMapStr : batchUserMaps) {
                    Map<Object, Object> objectMap = MapUtil.builder()
                            .put("rowNum", JSON.parseObject(batchUserMapStr).get("rowNum"))
                            .put("cause", "用户已领取该优惠券")
                            .build();
                    CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                            .batchId(event.getCouponTaskBatchId())
                            .jsonObject(com.alibaba.fastjson.JSON.toJSONString(objectMap))
                            .build();
                    couponTaskFailDOList.add(couponTaskFailDO);
                }

                // 添加到 t_coupon_task_fail 并标记错误原因
                couponTaskFailMapper.insert(couponTaskFailDOList);
            }
            long initId = 0;
            boolean isFirstIteration = true;  // 用于标识是否为第一次迭代
            String failFileAddress = excelPath + "/用户分发记录失败Excel-" + event.getCouponTaskBatchId() + ".xlsx";

            // 这里应该上传云 OSS 或者 MinIO 等存储平台，但是增加部署成功并且不太好往简历写就仅写入本地
            try (ExcelWriter excelWriter = EasyExcel.write(failFileAddress, UserCouponTaskFailExcelObject.class).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet("用户分发失败Sheet").build();
                while (true) {
                    List<CouponTaskFailDO> couponTaskFailDOList = listUserCouponTaskFail(event.getCouponTaskBatchId(), initId);
                    if (CollUtil.isEmpty(couponTaskFailDOList)) {
                        // 如果是第一次迭代且集合为空，则设置 failFileAddress 为 null
                        if (isFirstIteration) {
                            failFileAddress = null;
                        }
                        break;
                    }

                    // 标记第一次迭代已经完成
                    isFirstIteration = false;

                    // 将失败行数和失败原因写入 Excel 文件
                    List<UserCouponTaskFailExcelObject> excelDataList = couponTaskFailDOList.stream()
                            .map(each -> JSONObject.parseObject(each.getJsonObject(), UserCouponTaskFailExcelObject.class))
                            .toList();
                    excelWriter.write(excelDataList, writeSheet);

                    // 查询出来的数据如果小于 BATCH_USER_COUPON_SIZE 意味着后面将不再有数据，返回即可
                    if (couponTaskFailDOList.size() < BATCH_USER_COUPON_SIZE) {
                        break;
                    }

                    // 更新 initId 为当前列表中最大 ID
                    initId = couponTaskFailDOList.stream()
                            .mapToLong(CouponTaskFailDO::getId)
                            .max()
                            .orElse(initId);
                }
            }

            // 确保所有用户都已经接到优惠券后，设置优惠券推送任务完成时间
            CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                    .id(event.getCouponTaskId())
                    .status(CouponTaskStatusEnum.SUCCESS.getStatus())
                    .failFileAddress(failFileAddress)
                    .completionTime(new Date())
                    .build();
            couponTaskMapper.updateById(couponTaskDO);
        }

    }


    @SneakyThrows
    private void decrementCouponTemplateStockAndSaveUserCouponList(CouponTemplateDistributionEvent event) {
        // 如果等于 0 意味着已经没有了库存，直接返回即可
        Integer couponTemplateStock = decrementCouponTemplateStock(event, event.getBatchUserSetSize());
        if (couponTemplateStock <= 0) {
            return;
        }

        /**
         * 获取 Redis 中待保存入库用户优惠券列表
         * 缓存中存的数据格式为 Set
         *      Key -> coupon:cache:distribution:task-execute-batch-user:{couponTaskId}
         *      Value -> [{userId:xxx, rowNum:xxx}, {userId:xxx, rowNum:xxx}, {userId:xxx, rowNum:xxx}, {userId:xxx, rowNum:xxx}...]
         * 为了减少等待时间，读取 Excel 时将用户优惠券信息写入 Redis 中然后可以返回成功的信息，这里从 Redis 中获取数据执行入库操作
         */
        String batchUserSetKey = DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY + event.getCouponTaskId();
        List<String> batchUserMaps = stringRedisTemplate.opsForSet().pop(batchUserSetKey, couponTemplateStock);

        // 因为 batchUserIds 数据较多，ArrayList 会进行数次扩容，为了避免额外性能消耗，直接初始化 batchUserIds 大小的数组
        List<UserCouponDO> userCouponDOList = new ArrayList<>(batchUserMaps.size());
        Date now = new Date();

        for (String each : batchUserMaps) {
            JSONObject userIdAndRowNumJsonObject = JSON.parseObject(each);
            DateTime validEndTime = DateUtil.offsetHour(now, JSON.parseObject(event.getCouponTemplateConsumeRule()).getInteger("validityPeriod"));
            UserCouponDO userCouponDO = UserCouponDO.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .couponTemplateId(event.getCouponTemplateId())
                    .rowNum(userIdAndRowNumJsonObject.getInteger("rowNum"))
                    .userId(userIdAndRowNumJsonObject.getLong("userId"))
                    .receiveTime(now)
                    .receiveCount(1)
                    .validStartTime(now)
                    .validEndTime(validEndTime)
                    .source(CouponSourceEnum.PLATFORM.getType())
                    .status(CouponStatusEnum.EFFECTIVE.getType())
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            userCouponDOList.add(userCouponDO);
        }

        // 平台优惠券每个用户限领一次。批量新增用户优惠券记录。
        batchSaveUserCouponList(event.getCouponTemplateId(), event.getCouponTaskBatchId(), userCouponDOList);

        // 将这些优惠券添加到用户的领券记录中
        // 获取到所有用户的id
        List<String> userIdList = userCouponDOList.stream()
                .map(UserCouponDO::getUserId)
                .map(String::valueOf)
                .toList();
        String userIdsJson = new ObjectMapper().writeValueAsString(userIdList);

        List<String> couponIdList = userCouponDOList.stream()
                .map(each -> StrUtil.builder()
                        .append(event.getCouponTemplateId())
                        .append("_")
                        .append(each.getId())
                        .toString())
                .map(String::valueOf)
                .toList();
        String couponIdsJson = new ObjectMapper().writeValueAsString(couponIdList);
        // 调用 Lua 脚本时，传递参数
        List<String> keys = Arrays.asList(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY);
        List<String> args = Arrays.asList(userIdsJson, couponIdsJson, String.valueOf(new Date().getTime()));

        // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
        DefaultRedisScript<Void> buildLuaScript = Singleton.get(BATCH_SAVE_USER_COUPON_LUA_PATH, () -> {
            DefaultRedisScript<Void> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(BATCH_SAVE_USER_COUPON_LUA_PATH)));
            redisScript.setResultType(Void.class);
            return redisScript;
        });
        stringRedisTemplate.execute(buildLuaScript, keys, args.toArray());

    }

    private Integer decrementCouponTemplateStock(CouponTemplateDistributionEvent event, Integer decrementStockSize) {
        // 通过 MySQL 行级锁，对优惠券模板库存进行扣减
        Long couponTemplateId = event.getCouponTemplateId();
        int decremented = couponTemplateMapper.decrementCouponTemplateStock(event.getShopNumber(), couponTemplateId, decrementStockSize);
        // 如果扣减库存失败，意味着优惠券库存已不足，需要重试获取到可自减的库存数值
        if (!SqlHelper.retBool(decremented)) {
            LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                    .eq(CouponTemplateDO::getShopNumber, event.getShopNumber()).eq(CouponTemplateDO::getId, couponTemplateId);
            CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
            // 重试获取到可自减的库存数值
            return decrementCouponTemplateStock(event, couponTemplateDO.getStock());
        }
        return decrementStockSize;

    }

    private void batchSaveUserCouponList(Long couponTemplateId, Long couponTaskBatchId, List<UserCouponDO> userCouponDOList) {
        try {
            /**
             * MyBatisPlus 批量插入
             *  param1: 批量插入的集合
             *  param2: 批量插入的集合大小
             */
            userCouponMapper.insert(userCouponDOList, userCouponDOList.size());
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BatchExecutorException) {
                // 添加到  t_coupon_task_fail
                List<CouponTaskFailDO> couponTaskFailDOList = new ArrayList<>();
                List<UserCouponDO> toRemove = new ArrayList<>();

                // 调用批量新增失败后，为了避免大量重复失败，通过新增单条记录方式执行
                userCouponDOList.forEach(each -> {
                    try {
                        userCouponMapper.insert(each);
                    } catch (Exception ingored) {
                        Boolean hasReceived = couponExecuteDistributionConsumer.hasUserReceiveCoupon(couponTemplateId, each.getUserId());
                        if (hasReceived) {
                            // 添加到 t_coupon_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
                            Map<Object, Object> objectMap = MapUtil.builder().put("rowNum", each.getRowNum()).put("cause", "用户已领取该优惠券").build();
                            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                                    .batchId(couponTaskBatchId) // 失败的分发任务批次
                                    .jsonObject(JSON.toJSONString(objectMap)) // 具体失败原因
                                    .build();
                            couponTaskFailDOList.add(couponTaskFailDO);

                            // 从 userCouponDOList 中移除已经插入成功的记录
                            toRemove.add(each);
                        }
                    }
                });

                // 批量新增 t_coupon_task_fail 表
                couponTaskFailMapper.insert(couponTaskFailDOList, couponTaskFailDOList.size());

                // 删除已经重复的内容
                userCouponDOList.removeAll(toRemove);
                return;
            }
            throw ex;
        }


    }

    /**
     * 判断用户是否已经领取过优惠券
     *
     * @param couponTemplateId 优惠券模板ID
     * @param userId           用户ID
     * @return true: 用户已经领取过优惠券，false: 用户没有领取过优惠券
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Boolean hasUserReceiveCoupon(Long couponTemplateId, Long userId) {
        LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getCouponTemplateId, couponTemplateId);
        return userCouponMapper.selectOne(queryWrapper) != null;
    }


    /**
     * 批量获取用户优惠券失败记录
     *
     * @param batchId 分发任务批次 ID
     * @param maxId   上次读取最大 ID
     * @return 用户分发任务失败记录集合
     */
    private List<CouponTaskFailDO> listUserCouponTaskFail(Long batchId, Long maxId) {
        LambdaQueryWrapper<CouponTaskFailDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskFailDO.class)
                .eq(CouponTaskFailDO::getBatchId, batchId)
                .eq(CouponTaskFailDO::getId, maxId)
                .last("LIMIT " + BATCH_USER_COUPON_SIZE);
        return couponTaskFailMapper.selectList(queryWrapper);
    }


}
