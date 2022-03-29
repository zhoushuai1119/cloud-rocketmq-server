package com.cloud.platform.rocketmq.core;

import com.cloud.platform.rocketmq.annotation.AsyncSendTopic;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;

import javax.annotation.PostConstruct;

/**
 * 异步发送Executor
 * @param <T>
 */
@Data
@Slf4j
public abstract class AsyncSendExecutor<T> implements SendCallback {

    private String topic;
    private String eventCode;
    private RocketMQTemplate rocketMQTemplate;
    private SendCallback sendCallback;

    public AsyncSendExecutor(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 发送
     *
     * @param message  消息体
     * @return 发送结果
     */
    public void asyncSend(T message) {
        rocketMQTemplate.asyncSend(topic, eventCode, message, sendCallback);
    }

    @PostConstruct
    public void init() {
        AsyncSendTopic annotation = this.getClass().getAnnotation(AsyncSendTopic.class);
        topic = annotation.topic();
        eventCode = annotation.eventCode();
        this.sendCallback = this;
    }

}
