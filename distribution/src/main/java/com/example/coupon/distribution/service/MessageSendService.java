package com.example.coupon.distribution.service;

import com.example.coupon.distribution.dto.req.MessageSendReqDTO;
import com.example.coupon.distribution.dto.resp.MessageSendRespDTO;

/**
 * 消息发送接口 | 向用户推送优惠券消息 -> "您有一张 XXX 优惠券已到账，请在规定时间内使用..."
 */
public interface MessageSendService {

    /**
     * 消息发送接口
     */
    MessageSendRespDTO sendMessage(MessageSendReqDTO requestParam);
}
