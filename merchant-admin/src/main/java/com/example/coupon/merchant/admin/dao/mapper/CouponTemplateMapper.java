package com.example.coupon.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.coupon.merchant.admin.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券模板数据库持久层
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    /**
     * 增加优惠券模板发行量
     * @param couponTemplateId 优惠券模板 id
     * @param shopNumber 店铺 id
     * @param number 增加的数量
     */
    int increaseNumberCouponTemplate(@Param("couponTemplateId") String couponTemplateId,
                                     @Param("shopNumber") Long shopNumber,
                                     @Param("number") Integer number);

}
