package com.example.coupon.merchant.admin.controller;


import com.example.coupon.framework.idempotent.NoDuplicateSubmit;
import com.example.coupon.framework.result.Result;
import com.example.coupon.framework.web.Results;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.example.coupon.merchant.admin.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchant-admin/coupon-template")
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;


    @Operation(summary = "商家创建优惠券模板")
    @PostMapping("/create")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交优惠券模板")
    public Result<Void> createCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.createCouponTemplate(requestParam);
        return Results.success();
    }


    @Operation(summary = "商家增加优惠券模板发行量")
    @NoDuplicateSubmit(message = "请勿短时间内重复增加优惠券发行量")
    @PostMapping("/increase-number")
    public Result<Void> increaseNumberCouponTemplate(@RequestBody CouponTemplateNumberReqDTO requestParam) {
        couponTemplateService.increaseNumberCouponTemplate(requestParam);
        return Results.success();
    }


    @Operation(summary = "商家终止优惠券模板")
    @PostMapping("/terminate")
    public Result<Void> terminateCouponTemplate(String couponTemplateId) {
        couponTemplateService.terminateCouponTemplate(couponTemplateId);
        return Results.success();
    }





}
