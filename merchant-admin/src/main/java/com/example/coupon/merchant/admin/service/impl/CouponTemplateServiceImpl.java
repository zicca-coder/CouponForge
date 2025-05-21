package com.example.coupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coupon.merchant.admin.common.context.UserContext;
import com.example.coupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.example.coupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.merchant.admin.service.CouponTemplateService;
import com.example.coupon.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.coupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;


/**
 * 优惠券模板业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final MerchantAdminChainContext merchantAdminChainContext;


    /**
     * 创建优惠券模板
     * @param requestParam 请求参数
     */
    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

        // 新增优惠券模板信息到数据库
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        // 给优惠券模板设 生效 状态
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        // 给优惠券模板设置 店铺编码
        couponTemplateDO.setShopNumber(UserContext.getShopNumber());
        couponTemplateMapper.insert(couponTemplateDO);
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
        return null;
    }

    /**
     * 停用优惠券模板
     * @param couponTemplateId 优惠券模板id
     */
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {

    }

    /**
     * 增加优惠券模板库存
     * @param requestParam 请求参数
     */
    @Override
    public void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam) {

    }
}
