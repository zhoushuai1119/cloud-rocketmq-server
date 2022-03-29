package com.cloud.mq.base.dto;

import lombok.Data;

/**
 * 消息模板
 *
 */
@Data
public class CloudMessage<T> {
    /**
     * 消息id
     */
    String messageId;
    /**
     * topic
     */
    String topic;

    /**
     * key
     */
    String key;

    /**
     * eventCode
     */
    String eventCode;

    /**
     * 生成时间戳
     */
    long createTimeStamp;
    /**
     * 重试次数
     */
    int reconsumeTimes;

    /**
     * 事务id
     */
    String transactionId;

    /**
     * 消息内容
     */
    T payload;
}
