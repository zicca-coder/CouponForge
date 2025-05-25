package com.example.coupon.engine.controller;

import com.example.coupon.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.example.coupon.engine.service.CouponTemplateRemindService;
import com.example.coupon.framework.idempotent.NoDuplicateSubmit;
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
@Tag(name = "优惠券预约提醒管理")
@RequestMapping("/api/engine/coupon-template-remind")
public class CouponTemplateRemindController {

    private final CouponTemplateRemindService couponTemplateRemindService;

    @Operation(summary = "发出优惠券预约提醒请求")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交预约提醒请求")
    @PostMapping("/create")
    public Result<Void> createCouponRemind(@RequestBody CouponTemplateRemindCreateReqDTO requestParam) {
        couponTemplateRemindService.createCouponRemind(requestParam);
        return Results.success();
    }

}
