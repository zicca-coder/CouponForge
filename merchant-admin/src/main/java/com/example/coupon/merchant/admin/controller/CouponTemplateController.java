package com.example.coupon.merchant.admin.controller;


import com.example.coupon.framework.idempotent.NoDuplicateSubmit;
import com.example.coupon.framework.result.Result;
import com.example.coupon.framework.web.Results;
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
@RequestMapping("/api/merchant-admin")
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;


    @Operation(summary = "商家创建优惠券模板")
    @PostMapping("/coupon-template/create")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交优惠券模板")
    public Result<Void> createCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.createCouponTemplate(requestParam);
        return Results.success();
    }


}
