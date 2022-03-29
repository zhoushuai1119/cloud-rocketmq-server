package com.cloud.mq.base.core;


import com.cloud.mq.base.dto.CloudMessage;

/**
 * MQ消息监听
 * @param <T>
 */
public interface CloudMQListener<T> {
    /**
     * 接收消息.
     *
     * @param message
     */
    void onMessage(CloudMessage<T> message) throws Exception;
}
