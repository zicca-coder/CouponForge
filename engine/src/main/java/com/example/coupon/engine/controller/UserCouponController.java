package com.example.coupon.engine.controller;

import com.example.coupon.engine.dto.req.CouponCreatePaymentReqDTO;
import com.example.coupon.engine.dto.req.CouponProcessPaymentReqDTO;
import com.example.coupon.engine.dto.req.CouponProcessRefundReqDTO;
import com.example.coupon.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.example.coupon.engine.service.UserCouponService;
import com.example.coupon.framework.result.Result;
import com.example.coupon.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "用户优惠券管理")
@RequestMapping("/api/engine/user-coupon")
public class UserCouponController {

    private final UserCouponService userCouponService;

    @Operation(summary = "兑换优惠券模板", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/redeem")
    public Result<Void> redeemUserCoupon(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        userCouponService.redeemUserCoupon(requestParam);
        return Results.success();
    }

    @Operation(summary = "兑换优惠券模板之消息队列", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/redeem-mq")
    public Result<Void> redeemUserCouponByMQ(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        userCouponService.redeemUserCouponByMQ(requestParam);
        return Results.success();
    }

    @Operation(summary = "创建用户优惠券结算单", description = "用户下单时锁定使用的优惠券，一般由订单系统发起调用")
    @PostMapping("/create-payment-record")
    public Result<Void> createPaymentRecord(@RequestBody CouponCreatePaymentReqDTO requestParam) {
        userCouponService.createPaymentRecord(requestParam);
        return Results.success();
    }

    @Operation(summary = "核销优惠券结算单", description = "用户支付后核销使用的优惠券，常规来说应该监听支付后的消息队列事件")
    @PostMapping("/process-payment")
    public Result<Void> processPayment(@RequestBody CouponProcessPaymentReqDTO requestParam) {
        userCouponService.processPayment(requestParam);
        return Results.success();
    }

    @Operation(summary = "退款优惠券结算单", description = "用户退款成功后返回使用的优惠券，常规来说应该监听退款成功后的消息队列事件")
    @PostMapping("/process-refund")
    public Result<Void> processRefund(@RequestBody CouponProcessRefundReqDTO requestParam) {
        userCouponService.processRefund(requestParam);
        return Results.success();
    }


}
