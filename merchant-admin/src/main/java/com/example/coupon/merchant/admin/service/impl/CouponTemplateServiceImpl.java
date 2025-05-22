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
import org.springframework.stereotype.Service;

import java.sql.Wrapper;

import static com.example.coupon.merchant.admin.common.constant.CouponTemplateConstant.*;
import static com.example.coupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;


/**
 * 优惠券模板业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final CouponTemplateDelayExecuteStatusProducer  couponTemplateDelayExecuteStatusProducer;


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

        // 发送延时消息事件，优惠券模板活动到期修改优惠券模板状态
        CouponTemplateDelayEvent templateDelayEvent = CouponTemplateDelayEvent.builder()
                .shopNumber(UserContext.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .delayTime(couponTemplateDO.getValidEndTime().getTime())
                .build();
        couponTemplateDelayExecuteStatusProducer.sendMessage(templateDelayEvent);

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
