package com.example.coupon.engine.controller;

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


}
