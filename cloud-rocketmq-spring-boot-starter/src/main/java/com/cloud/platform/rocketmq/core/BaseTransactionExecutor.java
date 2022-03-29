package com.cloud.platform.rocketmq.core;

import com.cloud.platform.common.response.BaseResponse;
import com.cloud.platform.rocketmq.annotation.TansactionTopic;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * 事务消息Executor
 * @param <T>
 * @param <R>
 */
@Data
@Slf4j
public abstract class BaseTransactionExecutor<T, R> implements TopicTransactionListener<T, R> {

    private String topic;
    private String eventCode;
    private RocketMQTransactionTemplate rocketMQTransactionTemplate;

    public BaseTransactionExecutor(RocketMQTransactionTemplate rocketMQTransactionTemplate) {
        this.rocketMQTransactionTemplate = rocketMQTransactionTemplate;
    }

    /**
     * 发送
     *
     * @param message  消息体
     * @param localArg 本地事务参数
     * @return 发送结果
     */
    public BaseResponse<Object> send(T message, R localArg) {
        return rocketMQTransactionTemplate.send(topic, eventCode, message, localArg);
    }

    /**
     * 发送消息 指定key
     *
     * @param message 消息体
     * @return 发送结果
     */
    public BaseResponse<Object> send(T message, R localArg, String key) {
        return rocketMQTransactionTemplate.send(topic, eventCode, key, message, localArg);
    }

    @PostConstruct
    void init() throws Exception {
        TansactionTopic annotation = this.getClass().getAnnotation(TansactionTopic.class);
        topic = annotation.topic();
        eventCode = annotation.eventCode();
        DefaultTopicTransactionListenerImpl.registerListener(this);
    }

}
