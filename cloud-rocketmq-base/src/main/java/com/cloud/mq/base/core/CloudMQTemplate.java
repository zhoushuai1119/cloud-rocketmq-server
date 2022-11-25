
package com.cloud.mq.base.core;


import com.cloud.mq.base.dto.BatchMessage;
import com.cloud.mq.base.dto.PushMessage;
import com.cloud.platform.common.domain.response.BaseResponse;

/**
 * 消息发送模板接口
 */
public interface CloudMQTemplate {

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
     * @param timeoutMs 超时毫秒数
     * @return 发送结果
     */
    BaseResponse<Object> send(String topic, String eventCode, Object payload, long timeoutMs);

    /**
     * 同步发送延时消息
     *
     * @param topic          topic
     * @param eventCode      eventCode
     * @param payload        消息体
     * @param delayLevel 延时级别
     * @return 发送结果
     */
    BaseResponse<Object> send(String topic, String eventCode, Object payload, Integer delayLevel);


    /**
     * 根据hashBy的hash值发送至特定queue同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param hashBy    hash对象
     * @return 发送结果
     */
    BaseResponse<Object> sendToQueueByHash(String topic, String eventCode, Object payload, Object hashBy);

    /**
     * 根据hashBy的hash值发送至特定queue同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param hashBy    hash对象
     * @param key       key
     * @return 发送结果
     */
    BaseResponse<Object> sendToQueueByHash(String topic, String eventCode, String key, Object payload, Object hashBy);

    /**
     * pushMessage 发送消息
     *
     * @return 发送结果
     */
    BaseResponse<Object> send(PushMessage pushMessage);

    /**
     * 批量发送
     *
     * @param batchMessages 消息列表
     * @return 发送结果
     */
    BaseResponse<Object> sendBatch(BatchMessage batchMessages);

    /**
     * 单向发送
     *
     * @param pushMessage 消息
     */
    void sendOneway(PushMessage pushMessage);


    /**
     * 单向发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     */
    void sendOneway(String topic, String eventCode, Object payload);
}
