package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.dto.CloudMessage;

/**
 * 分topic消费的接口
 *
 * @author shuai
 */
public interface TopicListener<T> {

    /**
     * 接收消息.
     *
     * @param message the message
     * @throws Exception the exception
     */
    void onMessage(CloudMessage<T> message) throws Exception;

}
