package com.example.coupon.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "优惠券推送任务分页查询参数")
public class CouponTaskPageQueryReqDTO extends Page {
    /**
     * 批次id
     */
    @Schema(description = "批次id")
    private String batchId;

    /**
     * 优惠券批次任务名称
     */
    @Schema(description = "优惠券批次任务名称")
    private String taskName;

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id")
    private String couponTemplateId;

    /**
     * 状态 0：待执行 1：执行中 2：执行失败 3：执行成功 4：取消
     */
    @Schema(description = "状态 0：待执行 1：执行中 2：执行失败 3：执行成功 4：取消")
    private Integer status;
}
