package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.dto.CloudMessage;

/**
 * 分topic消费的接口
 *
 */
public interface TopicListener<T> {

    /**
     * 接收消息.
     *
     * @param message
     */
    void onMessage(CloudMessage<T> message) throws Exception;
}
