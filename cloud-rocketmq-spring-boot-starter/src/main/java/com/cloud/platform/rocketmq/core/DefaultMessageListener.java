package com.cloud.platform.rocketmq.core;

import com.cloud.mq.base.core.CloudMQListener;
import com.cloud.mq.base.dto.CloudMessage;
import com.cloud.platform.rocketmq.utils.MqMessageUtil;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * 默认的listener.
 */
@Slf4j
public class DefaultMessageListener implements CloudMQListener<String> {
    /**
     * key 为 topicAndEventCode
     * ConsumeTopicInfo 有对应的消费逻辑 listener
     */
    private Map<String, ConsumeTopicInfo> topicConsumerMap;

    public DefaultMessageListener(Map<String, ConsumeTopicInfo> topicConsumerMap) {
        this.topicConsumerMap = topicConsumerMap;
        if (topicConsumerMap != null) {
            for (ConsumeTopicInfo topicInfo : topicConsumerMap.values()) {
                //设置消息转化类型
                topicInfo.setMessageType(MqMessageUtil.getMessageJavaType2Topic(topicInfo.getTopicListener()));
            }
        }
    }

    @Override
    public void onMessage(CloudMessage<String> message) throws Exception {
        //查询是否有 *,避免多个eventCode配置干扰
        ConsumeTopicInfo consume = topicConsumerMap.get(message.getTopic() + ":*");
        if (Objects.isNull(consume)) {
            String key = message.getTopic() + ":" + message.getEventCode();
            consume = topicConsumerMap.get(key);
        }
        if (Objects.nonNull(consume)) {
            if (consume.isLog()) {
                log.info("receive mq topic:{}, eventCode:{}, payload:{}", message.getTopic(), message.getEventCode(), message.getPayload());
            }
            consume.getTopicListener().onMessage(MqMessageUtil.convertMessage(message, consume.getMessageType()));
        } else {
            log.error("fail to find TopicListener for {}, payload {}", message.getTopic(), message.getPayload());
        }
    }

    /**
     * topic消费信息
     */
    @Data
    public static final class ConsumeTopicInfo {
        /**
         * topic
         */
        private String topic;

        /**
         * eventCode
         */
        private String eventCode;

        /**
         * 是否打印日志
         */
        private boolean log;

        /**
         * listener
         */
        private TopicListener topicListener;

        /**
         * 消息的json转换类型
         */
        private JavaType messageType;
    }
}
