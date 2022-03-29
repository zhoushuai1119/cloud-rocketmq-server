package com.cloud.mq.base.dto;

import com.cloud.mq.base.enums.DelayLevelEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送消息
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {

    /**
     * topic
     */
    String topic;

    /**
     * eventCode
     */
    String eventCode;

    /**
     * key
     */
    String key;
    /**
     * 延时级别
     */
    DelayLevelEnum delayTimeLevel;
    /**
     * 超时毫秒数
     */
    Integer timeoutMs;
    /**
     * HASH对象
     */
    Object hashBy;

    /**
     * 消息内容
     */
    Object payload;
}
