package com.example.coupon.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.coupon.merchant.admin.dao.entity.CouponTaskDO;
import com.example.coupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTaskPageQueryReqDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTaskQueryRespDTO;

/**
 * 优惠券推送业务逻辑层
 */
public interface CouponTaskService extends IService<CouponTaskDO> {

    /**
     * 商家创建优惠券推送任务
     * @param requestParam 请求参数
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);

    /**
     * 分页查询商家优惠券推送任务
     * @param requestParam 请求参数
     * @return 商家优惠券推送分页任务
     */
    IPage<CouponTaskQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam);


    /**
     * 根据任务ID查询任务详情
     * @param taskId 推送任务 ID
     * @return 任务详情
     */
    CouponTaskQueryRespDTO findCouponTaskById(String taskId);



}
