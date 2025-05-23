package com.example.coupon.engine.controller;

import com.example.coupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.engine.service.CouponTemplateService;
import com.example.coupon.framework.result.Result;
import com.example.coupon.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板管理")
@RequestMapping("/api/engine/coupon-template")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "查询优惠券模板")
    @GetMapping("/query")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.findCouponTemplate(requestParam));
    }
}
