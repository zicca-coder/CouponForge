package com.example.coupon.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "优惠券推送任务查询返回实体")
public class CouponTaskQueryRespDTO {
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
     * 发放优惠券数量
     */
    @Schema(description = "发放优惠券数量")
    private Integer sendNum;

    /**
     * 通知方式，可组合使用 0：站内信 1：弹框推送 2：邮箱 3：短信
     */
    @Schema(description = "通知方式，0：站内信 1：弹框推送 2：邮箱 3：短信")
    private String notifyType;

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id")
    private String couponTemplateId;

    /**
     * 发送类型 0：立即发送 1：定时发送
     */
    @Schema(description = "发送类型，0：立即发送 1：定时发送")
    private Integer sendType;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    /**
     * 状态 0：待执行 1：执行中 2：执行失败 3：执行成功 4：取消
     */
    @Schema(description = "状态 0：待执行 1：执行中 2：执行失败 3：执行成功 4：取消")
    private Integer status;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completionTime;

    /**
     * 操作人
     */
    @Schema(description = "操作人")
    private Long operatorId;
}
