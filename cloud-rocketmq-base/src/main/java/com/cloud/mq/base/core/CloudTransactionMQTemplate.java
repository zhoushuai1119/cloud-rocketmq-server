
package com.cloud.mq.base.core;


import com.cloud.platform.common.response.BaseResponse;

/**
 * 消息发送模板接口
 *
 */
public interface CloudTransactionMQTemplate {
    /**
     * 同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @return 发送结果
     */
    BaseResponse<Object> send(String topic, String eventCode, Object payload);

    /**
     * 同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param arg       Custom business parameter
     * @return 发送结果
     */
    BaseResponse<Object> send(String topic, String eventCode, Object payload, Object arg);

    /**
     * 同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param key key
     * @param payload   消息体
     * @param arg       Custom business parameter
     * @return 发送结果
     */
    BaseResponse<Object> send(String topic, String eventCode, String key, Object payload, Object arg);

}
