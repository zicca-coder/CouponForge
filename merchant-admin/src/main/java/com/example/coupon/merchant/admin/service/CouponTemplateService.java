package com.example.coupon.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.coupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 创建优惠券模板
     * @param requestParam 请求参数
     */
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);

    /**
     * 分页查询商家优惠券模板
     * @param requestParam 请求参数
     * @return 商家优惠券模板分页数据
     */
    IPage<CouponTemplatePageQueryRespDTO> queryCouponTemplatePage(CouponTemplatePageQueryReqDTO requestParam);

    /**
     * 查询优惠券模板详情
     * Note: 后管接口不存在高并发情况，因此直接查询数据库即可，不需要做缓存
     * @param couponTemplateId 优惠券模板id
     */
    CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId);

    /**
     * 结束优惠券模板
     * @param couponTemplateId 优惠券模板id
     */
    void terminateCouponTemplate(String couponTemplateId);

    /**
     * 增加优惠券模板发行量
     * @param requestParam 请求参数
     */
    void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam);



}
