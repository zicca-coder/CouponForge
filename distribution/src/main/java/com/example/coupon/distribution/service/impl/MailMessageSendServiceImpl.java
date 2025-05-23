package com.example.coupon.distribution.service.impl;

import com.example.coupon.distribution.common.enums.SendMessageMarkCovertEnum;
import com.example.coupon.distribution.dto.req.MessageSendReqDTO;
import com.example.coupon.distribution.dto.resp.MessageSendRespDTO;
import com.example.coupon.distribution.service.MessageSendService;
import com.example.coupon.distribution.service.basics.DistributionExecuteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 邮件消息发送接口实现类
 */
@Slf4j
@Service
public class MailMessageSendServiceImpl implements MessageSendService, DistributionExecuteStrategy<MessageSendReqDTO, MessageSendRespDTO> {
    @Override
    public String mark() {
        return SendMessageMarkCovertEnum.EMAIL.name();
    }

    @Override
    public MessageSendRespDTO executeResp(MessageSendReqDTO requestParam) {
        return sendMessage(requestParam);
    }

    @Override
    public MessageSendRespDTO sendMessage(MessageSendReqDTO requestParam) {
        log.info("发送了一条【邮件】推送消息......");
        return null;
    }
}
