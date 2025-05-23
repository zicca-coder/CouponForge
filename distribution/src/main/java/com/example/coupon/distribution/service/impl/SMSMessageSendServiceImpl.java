package com.example.coupon.distribution.service.impl;

import com.example.coupon.distribution.common.enums.SendMessageMarkCovertEnum;
import com.example.coupon.distribution.dto.req.MessageSendReqDTO;
import com.example.coupon.distribution.dto.resp.MessageSendRespDTO;
import com.example.coupon.distribution.service.MessageSendService;
import com.example.coupon.distribution.service.basics.DistributionExecuteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信发送服务实现
 */
@Slf4j
@Service
public class SMSMessageSendServiceImpl implements MessageSendService, DistributionExecuteStrategy<MessageSendReqDTO, MessageSendRespDTO> {

    @Override
    public MessageSendRespDTO sendMessage(MessageSendReqDTO requestParam) {
        log.info("发送了一条【短信】推送消息......");
        return null;
    }

    @Override
    public String mark() {
        return SendMessageMarkCovertEnum.SMS.name();
    }

    @Override
    public MessageSendRespDTO executeResp(MessageSendReqDTO requestParam) {
        return sendMessage(requestParam);
    }
}
