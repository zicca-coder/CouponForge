package com.example.coupon.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;
import com.example.coupon.merchant.admin.common.enums.DiscountTargetEnum;
import com.example.coupon.merchant.admin.common.enums.DiscountTypeEnum;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.example.coupon.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import static com.example.coupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;

/**
 * 验证优惠券创建接口参数是否正确责任链组件 | 验证参数数据是否符合逻辑关系
 */
@Component
public class CouponTemplateCreateParamVerifyChainFilter implements MerchantAdminAbstractChainHandler<CouponTemplateSaveReqDTO> {
    @Override
    public void handler(CouponTemplateSaveReqDTO requestParam) {
        if (ObjectUtil.equal(requestParam.getTarget(), DiscountTargetEnum.PRODUCT_SPECIFIC.getType())) {
            // 后续可以通过商品服务查询商品信息，判断商品是否存在，商品是否上架，商品是否属于商家
            //
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
