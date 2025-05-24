package com.example.coupon.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.coupon.distribution.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券模板持久层
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {


    /**
     * 自减优惠券模板库存，防止超卖
     * @param shopNumber 店铺 id
     * @param couponTemplateId 优惠券模板 id
     * @param decrementStock 要扣减的库存
     * @return 影响行数
     */
    int decrementCouponTemplateStock(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") Long couponTemplateId, @Param("decrementStock") Integer decrementStock);


}
