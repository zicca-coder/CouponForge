package com.example.coupon.engine.mq.event;

import com.example.coupon.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券兑换事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponRedeemEvent {

    /**
     * Web 请求参数
     */
    private CouponTemplateRedeemReqDTO requestParam;

    /**
     * 领取次数
     */
    private Integer receiveCount;

    /**
     * 优惠券模板
     */
    private CouponTemplateQueryRespDTO couponTemplate;

    /**
     * 用户 ID
     */
    private String userId;
}
