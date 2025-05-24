package com.example.coupon.distribution.service.handler.excel;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.coupon.distribution.common.constant.DistributionRedisConstant;
import com.example.coupon.distribution.common.constant.EngineRedisConstant;
import com.example.coupon.distribution.dao.entity.CouponTaskDO;
import com.example.coupon.distribution.dao.entity.CouponTaskFailDO;
import com.example.coupon.distribution.dao.entity.CouponTemplateDO;
import com.example.coupon.distribution.dao.mapper.CouponTaskFailMapper;
import com.example.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.example.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.example.coupon.distribution.toolkit.StockDecrementReturnCombinedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Map;

/**
 * 优惠券任务读取 Excel 分发监听器
 */
@Slf4j
@RequiredArgsConstructor
public class ReadExcelDistributionListener extends AnalysisEventListener<CouponTaskExcelObject> {

    private final CouponTaskDO couponTaskDO;
    private final CouponTemplateDO couponTemplateDO;
    private final CouponTaskFailMapper couponTaskFailMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponExecuteDistributionProducer couponExecuteDistributionProducer;

    private int rowCount = 1;
    private final static String STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH = "lua/stock_decrement_and_batch_save_user_record.lua";
    private final static int BATCH_USER_COUPON_SIZE = 50;


    @Override
    public void invoke(CouponTaskExcelObject data, AnalysisContext context) {
        Long couponTaskId = couponTaskDO.getId();

        // 获取当前【推送任务】的执行进度，判断是否已经执行过。如果已执行，则跳过即可，防止执行到一半应用宕机
        String templateTaskExecuteProgressKey = DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY + couponTaskId;
        String progress = stringRedisTemplate.opsForValue().get(templateTaskExecuteProgressKey);
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) >= rowCount) {
            ++rowCount;
            return;
        }

        // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
        DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH)));
            redisScript.setResultType(Long.class);
            return redisScript;
        });

        // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录
        String couponTemplateKey = EngineRedisConstant.COUPON_TEMPLATE_KEY + couponTemplateDO.getId();
        log.error(couponTemplateKey);
        String batchUserSetKey = DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY + couponTaskId;
        Map<Object, Object> userRowNumMap = MapUtil.builder()
                .put("userId", data.getUserId())
                .put("rowNum", rowCount + 1)
                .build();
        Long combinedField = stringRedisTemplate.execute(buildLuaScript, ListUtil.of(couponTemplateKey, batchUserSetKey), JSON.toJSONString(userRowNumMap));

        // firstField 为 false，说明没有库存了
        boolean firstField = StockDecrementReturnCombinedUtil.extractFirstField(combinedField);
        if (!firstField) {
            // 同步当前执行进度到缓存中
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
            ++rowCount;

            // 添加到 t_coupon_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
            Map<Object, Object> objectMap = MapUtil.builder()
                    .put("rowNum", rowCount + 1)
                    .put("cause", "优惠券模板无库存")
                    .build();
            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                    .batchId(couponTaskDO.getBatchId())
                    .jsonObject(JSON.toJSONString(objectMap, SerializerFeature.WriteMapNullValue))
                    .build();
            couponTaskFailMapper.insert(couponTaskFailDO);
            return;
        }

        // 获取用户领券集合长度
        int batchUserSetSize = StockDecrementReturnCombinedUtil.extractSecondField(combinedField.intValue());

        // 如果没有消息通知需求，仅在 batchUserSetSize = BATCH_USER_COUPON_SIZE 时才发送消息消费，不满足条件仅记录进度即可
        if (batchUserSetSize < BATCH_USER_COUPON_SIZE && StrUtil.isBlank(couponTaskDO.getNotifyType())) {
            // 同步当前 Excel 执行进度到缓存
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
            ++rowCount;
            return;
        }

        CouponTemplateDistributionEvent couponTemplateDistributionEvent = CouponTemplateDistributionEvent.builder()
                .userId(data.getUserId())
                .mail(data.getMail())
                .phone(data.getPhone())
                .couponTaskId(couponTaskId)
                .notifyType(couponTaskDO.getNotifyType())
                .shopNumber(couponTaskDO.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .couponTaskBatchId(couponTaskDO.getBatchId())
                .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                .batchUserSetSize(batchUserSetSize)
                .distributionEndFlag(Boolean.FALSE)
                .build();
        // 发送消息开始推送
        couponExecuteDistributionProducer.sendMessage(couponTemplateDistributionEvent);

        // 同步当前执行进度到缓存
        stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
        ++rowCount;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 发送 Excel 解析完成标识，即使不满足批量保存的数量也得保存到数据库
        CouponTemplateDistributionEvent couponTemplateDistributionEvent = CouponTemplateDistributionEvent.builder()
                .distributionEndFlag(Boolean.TRUE)
                .shopNumber(couponTaskDO.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                .couponTaskBatchId(couponTaskDO.getBatchId())
                .couponTaskId(couponTaskDO.getId())
                .build();
        couponExecuteDistributionProducer.sendMessage(couponTemplateDistributionEvent);
    }
}
