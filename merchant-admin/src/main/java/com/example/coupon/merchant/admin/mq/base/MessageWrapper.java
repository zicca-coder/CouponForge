package com.example.coupon.merchant.admin.mq.base;

import lombok.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * 消息体包装器
 * @param <T> 消息体类型
 */
@Data
@NoArgsConstructor(force = true) // 自动生成一个无参构造函数，即使类中有final字段，默认情况下，如果类中有final字段，Lombok不会生成无参构造，因为这些字段必须被初始化
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
public final class MessageWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息发送 Keys
     */
    @Nonnull // 表明该字段可以为空
    private String keys;

    /**
     * 消息体
     */
    @Nonnull
    private T message;

    /**
     * 消息发送时间
     */
    private Long timestamp = System.currentTimeMillis();
}
