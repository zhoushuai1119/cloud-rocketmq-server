package com.cloud.mq.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量消息-消息内容
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MQMessage {

    /**
     * eventCode
     */
    String eventCode;

    /**
     * key
     */
    String key;

    /**
     * 消息内容
     */
    Object payload;
}
