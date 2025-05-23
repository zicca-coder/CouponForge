package com.example.coupon.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.coupon.engine.dao.entity.CouponTemplateDO;
import com.example.coupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;

import java.util.List;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 查询优惠券模板信息
     *
     * @param requestParam 请求参数
     * @return 优惠券模板信息
     */
    CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam);


    /**
     * 根据优惠券id集合查询出券信息
     *
     * @param couponTemplateIds 优惠券id集合
     */
    List<CouponTemplateDO> listCouponTemplateByIds(List<Long> couponTemplateIds, List<Long> shopNumbers);
}
