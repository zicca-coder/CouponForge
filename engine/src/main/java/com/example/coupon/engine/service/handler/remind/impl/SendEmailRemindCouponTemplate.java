package com.example.coupon.engine.service.handler.remind.impl;

import com.example.coupon.engine.service.handler.remind.RemindCouponTemplate;
import com.example.coupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "SendEmailRemindCouponTemplate")
public class SendEmailRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 以邮件方式提醒用户抢券
     *
     * @param couponTemplateRemindDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(CouponTemplateRemindDTO couponTemplateRemindDTO) {
        // 空实现
        log.info("【邮件推送方式】提醒[用户-{}]抢购【优惠券-{}】", couponTemplateRemindDTO.getUserId(), couponTemplateRemindDTO.getCouponTemplateId());
        return true;
    }
}
