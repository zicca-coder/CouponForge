package com.example.coupon.merchant.admin.controller;

import com.example.coupon.framework.idempotent.NoDuplicateSubmit;
import com.example.coupon.framework.result.Result;
import com.example.coupon.framework.web.Results;
import com.example.coupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.example.coupon.merchant.admin.service.CouponTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券推送任务管理")
@RequestMapping("/api/merchant-admin/coupon-task/")
public class CouponTaskController {

    private final CouponTaskService couponTaskService;


    @Operation(summary = "商家创建优惠券推送任务")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交优惠券推送任务")
    @PostMapping("/create")
    public Result<Void> createCouponTask(@RequestBody CouponTaskCreateReqDTO requestParam) {
        couponTaskService.createCouponTask(requestParam);
        return Results.success();
    }





}
