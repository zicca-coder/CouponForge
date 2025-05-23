package com.example.coupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.example.coupon.framework.exception.ClientException;
import com.example.coupon.framework.exception.ServiceException;
import com.example.coupon.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.example.coupon.merchant.admin.common.context.UserContext;
import com.example.coupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.example.coupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import com.example.coupon.merchant.admin.mq.producer.CouponTemplateDelayExecuteStatusProducer;
import com.example.coupon.merchant.admin.service.CouponTemplateService;
import com.example.coupon.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.coupon.merchant.admin.common.constant.CouponTemplateConstant.*;
import static com.example.coupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;
import static com.example.coupon.merchant.admin.common.enums.ChainBizMarkEnum.valueOf;


/**
 * 优惠券模板业务逻辑实现层
 */
@Slf4j(topic = "CouponTemplateServiceImpl")
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final CouponTemplateDelayExecuteStatusProducer  couponTemplateDelayExecuteStatusProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final RBloomFilter<String> couponTemplateQueryBloomFilter;



    /**
     * 创建优惠券模板
     * @param requestParam 请求参数
     */
    @LogRecord(
            success = CREATE_COUPON_TEMPLATE_LOG_CONTENT, // 方法执行成功后的日志模板
            type = "CouponTemplate", // 操作日志的类型
            bizNo = "{{#binNo}}", // 日志绑定的业务标识
            extra = "{{#requestParam.toString()}}" // 额外的信息
    )
    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);
        LogRecordContext.putVariable("originalData", JSON.toJSONString(requestParam));

        // 新增优惠券模板信息到数据库
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        // 给优惠券模板设 生效 状态
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        // 给优惠券模板设置 店铺编码
        couponTemplateDO.setShopNumber(UserContext.getShopNumber());
        couponTemplateMapper.insert(couponTemplateDO);

        // 因为模板 ID 是运行中生成的，@LogRecord 默认拿不到，因此需要手动设置
        LogRecordContext.putVariable("binNo", couponTemplateDO.getId());


        // 插入数据库之后进行缓存预热，将优惠券模板信息缓存到 Redis 中
        // 将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
        // 将 CouponTemplateQueryRespDTO 转换成 Map，方便后续以哈希格式存储到 Redis 中
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
        // 将 Map 转换成 Map<String, String>，方便后续以哈希格式存储到 Redis 中
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());

        // 通过 LUA 脚本设置 Hahs 数据以及设置过期时间
        /**
         * HSET : 将多个字段和值写入一个 Redis Hash
         *  KEYS[1] : 要操作的 Redis Key
         *  ARGV: 传进来的参数数组
         *  #ARGV: ARGV 数组的长度，参数个数
         *  upack(ARGV, 1, #ARGV - 1) 从参数数组 ARGV 中取出第一个到倒数第二个参数
         * EXPIREAT : 为 KEYS[1] 设置一个过期时间， 过期时间为  ARGV[#ARGV]
         *  ARGV[#ARGV]: 参数数组中的最后一个参数
         */
        String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

        List<String> keys = Collections.singletonList(couponTemplateCacheKey);
        List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
        actualCacheTargetMap.forEach((key, value) -> {
            args.add(key);
            args.add(value);
        });

        // 将优惠券活动过期时间设置为 Redis 缓存的过期时间
        // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
        args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

        // 执行LUA脚本
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                keys,
                args.toArray()
        );


        // 发送延时消息事件，优惠券模板活动到期修改优惠券模板状态
        CouponTemplateDelayEvent templateDelayEvent = CouponTemplateDelayEvent.builder()
                .shopNumber(UserContext.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .delayTime(couponTemplateDO.getValidEndTime().getTime())
                .build();
        couponTemplateDelayExecuteStatusProducer.sendMessage(templateDelayEvent);


        // 添加优惠券模板 ID 到布隆过滤器中，用于查询优惠券模板是否存在
        log.info("优惠券模板ID:{}已添加至【布隆过滤器】...", couponTemplateDO.getId());
        couponTemplateQueryBloomFilter.add(String.valueOf(couponTemplateDO.getId()));

    }

    /**
     * 分页查询优惠券模板
     * @param requestParam 请求参数
     */
    @Override
    public IPage<CouponTemplatePageQueryRespDTO> queryCouponTemplatePage(CouponTemplatePageQueryReqDTO requestParam) {
        return null;
    }

    /**
     * 查询优惠券模板详情
     * @param couponTemplateId 优惠券模板id
     */
    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {
        CouponTemplateDO couponTemplateDO = lambdaQuery().eq(CouponTemplateDO::getId, couponTemplateId)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .one();
        return BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
    }

    /**
     * 停用优惠券模板
     * @param couponTemplateId 优惠券模板id
     */
    @LogRecord(
            success = TERMINATE_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#couponTemplateId}}"
    )
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {
        // 验证是否存在数据横向越权
        CouponTemplateDO couponTemplateDO = lambdaQuery().eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, couponTemplateId)
                .one();
        if (couponTemplateDO == null) {
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }

        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已停用，请勿重复操作...");
        }

        // 记录优惠券模板修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        // 修改优惠券模板状态为结束状态
        lambdaUpdate().eq(CouponTemplateDO::getId, couponTemplateId)
                .eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .set(CouponTemplateDO::getStatus, CouponTemplateStatusEnum.ENDED.getStatus())
                .update();
    }

    /**
     * 增加优惠券模板库存
     * @param requestParam 请求参数
     */
    @LogRecord(
            success = INCREASE_NUMBER_COUPON_TEMPLATE_LOG_CONTENT,
            type = "CouponTemplate",
            bizNo = "{{#requestParam.couponTemplateId}}"
    )
    @Override
    public void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam) {
        // 验证是否存在数据横向越权
        CouponTemplateDO couponTemplateDO = lambdaQuery().eq(CouponTemplateDO::getShopNumber, UserContext.getShopNumber())
                .eq(CouponTemplateDO::getId, requestParam.getCouponTemplateId())
                .one();
        if (couponTemplateDO == null) {
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }
        // 验证优惠券模板是否已经结束
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已停用，请勿重复操作...");
        }
        // 记录优惠券模板修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));

        int increated = couponTemplateMapper.increaseNumberCouponTemplate(requestParam.getCouponTemplateId(), UserContext.getShopNumber(), requestParam.getNumber());
        if (!SqlHelper.retBool(increated)) {
            throw new ServiceException("优惠券模板增加发行量失败...");
        }

    }
}
