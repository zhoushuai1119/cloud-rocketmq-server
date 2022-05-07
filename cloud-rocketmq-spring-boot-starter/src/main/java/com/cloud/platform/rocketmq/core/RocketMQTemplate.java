package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.core.CloudMQTemplate;
import com.cloud.mq.base.dto.BatchMessage;
import com.cloud.mq.base.dto.PushMessage;
import com.cloud.mq.base.utils.BatchMsgUtil;
import com.cloud.mq.base.utils.ValuesUtil;
import com.cloud.mq.base.enums.DelayLevelEnum;
import com.cloud.platform.common.domain.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByRandom;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.cloud.platform.rocketmq.utils.MqMessageUtil.convertToRocketMsg;


/**
 * rocketMQ发送模板
 *
 */
@Slf4j
public class RocketMQTemplate implements CloudMQTemplate, InitializingBean, DisposableBean {

    @Getter
    @Setter
    private DefaultMQProducer producer;

    @Getter
    @Setter
    private String charset = "UTF-8";

    //随机选择队列
    private static final MessageQueueSelector SELECT_QUEUE_BY_RANDOM = new SelectMessageQueueByRandom();
    //hash选择队列
    private static final MessageQueueSelector SELECT_QUEUE_BY_HASH = new SelectMessageQueueByHash();

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(producer, "Property 'producer' is required");
        producer.start();
    }

    @Override
    public void destroy() {
        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }
    }


    /**
     * 同步发送延时消息
     *
     * @param topic          topic
     * @param eventCode      eventCode
     * @param payload        消息体
     * @param delayTimeLevel 延时级别
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, Object payload, DelayLevelEnum delayTimeLevel) {
        return send(topic, eventCode, null, payload, null, producer.getSendMsgTimeout(), delayTimeLevel.getCode());
    }


    @Override
    public void asyncSend(String topic, String eventCode, Object payload, SendCallback sendCallback) {
         asyncSend(topic, eventCode, payload, producer.getSendMsgTimeout(),sendCallback);
    }

    /**
     * 发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, Object payload) {
        return send(topic, eventCode, null, payload, null, producer.getSendMsgTimeout(), null);
    }

    /**
     * 发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param timeoutMs 超时毫秒数
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, Object payload, long timeoutMs) {
        return send(topic, eventCode, null, payload, null, timeoutMs, null);
    }

    /**
     * 根据hashBy的hash值发送至特定queue同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param hashBy    hash对象
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> sendToQueueByHash(String topic, String eventCode, Object payload, Object hashBy) {
        return send(topic, eventCode, null, payload, hashBy, producer.getSendMsgTimeout(), null);
    }


    /**
     * 发送消息 单向发送
     *
     * @param pushMessage 消息
     */
    @Override
    public void sendOneway(PushMessage pushMessage) {
        sendOneway(pushMessage.getTopic(), pushMessage.getEventCode(), pushMessage.getKey(), pushMessage.getPayload(),
                pushMessage.getHashBy(),
                ValuesUtil.nullOrDefault(pushMessage.getTimeoutMs(), producer.getSendMsgTimeout()),
                ValuesUtil.nullOrDefault(pushMessage.getDelayTimeLevel(), null));
    }

    /**
     * 发送消息 单向发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     */
    @Override
    public void sendOneway(String topic, String eventCode, Object payload) {
        sendOneway(topic, eventCode, null, payload, null, producer.getSendMsgTimeout(), null);
    }

    /**
     * 根据hashBy的hash值发送至特定queue同步发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param key       key
     * @param payload   消息体
     * @param hashBy    hash对象
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> sendToQueueByHash(String topic, String eventCode, String key, Object payload, Object hashBy) {
        return send(topic, eventCode, key, payload, hashBy, producer.getSendMsgTimeout(), null);
    }

    /**
     * pushMessage 发送消息
     *
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(PushMessage pushMessage) {
        String topic = pushMessage.getTopic();
        String eventCode = pushMessage.getEventCode();
        String key = pushMessage.getKey();
        Object payload = pushMessage.getPayload();
        Object hashBy = pushMessage.getHashBy();

        Integer delay = null;
        if (Objects.nonNull(pushMessage.getDelayTimeLevel())) {
            delay = pushMessage.getDelayTimeLevel().getCode();
        }

        Integer timeout = pushMessage.getTimeoutMs();
        if (Objects.isNull(timeout) || timeout < 0) {
            timeout = producer.getSendMsgTimeout();
        }

        return send(topic, eventCode, key, payload, hashBy, timeout, delay);
    }

    /**
     * 批量发送
     *
     * @param batchMessages 消息列表
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> sendBatch(BatchMessage batchMessages) {
        String topic = batchMessages.getTopic();
        ValuesUtil.checkTopic(topic);
        List<Message> messageList = batchMessages.getMqMessages().stream().map(mqMessage -> {
                    ValuesUtil.checkEventCode(mqMessage.getEventCode());
                    return convertToRocketMsg(topic, mqMessage.getEventCode(), mqMessage.getKey(),
                            mqMessage.getPayload());
                }
        ).collect(Collectors.toList());

        MessageBatch messageBatch;
        try {
            messageBatch = BatchMsgUtil.batch(messageList, producer);
        } catch (MQClientException e) {
            log.info("Failed to initiate the MessageBatch,topic:{}", topic);
            throw new MessagingException(e.getMessage(), e);
        }
        SendResult sendResult;
        try {
            long now = System.currentTimeMillis();
            Object hashBy = batchMessages.getHashBy();
            int timeoutMs = ValuesUtil.nullOrDefault(batchMessages.getTimeoutMs(), producer.getSendMsgTimeout());
            if (hashBy == null) {
                sendResult = producer.send(messageBatch, SELECT_QUEUE_BY_RANDOM, timeoutMs);
            } else {
                sendResult = producer.send(messageBatch, SELECT_QUEUE_BY_HASH, hashBy, timeoutMs);
            }
            long costTime = System.currentTimeMillis() - now;
            if (log.isDebugEnabled()) {
                log.debug("send batchMessage cost: {} ms", costTime);
            }

            return handleSendResult(sendResult);
        } catch (Exception e) {
            log.info("sendBatch failed. topic:{}, message:{} ,exception:{}", batchMessages.getTopic(), messageList,
                    e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }


    /**
     * 发送
     *
     * @param topic          topic
     * @param eventCode      eventCode
     * @param payload        消息体
     * @param hashBy         根据hash值发送至特定queue
     * @param timeoutMs      超时毫秒数
     * @param delayTimeLevel 延时消息
     * @return 发送结果
     */
    private BaseResponse<Object> send(String topic, String eventCode, String key, Object payload, Object hashBy,
                                      long timeoutMs, Integer delayTimeLevel) {
        ValuesUtil.checkTopicAndEventCode(topic, eventCode);
        try {
            long now = System.currentTimeMillis();
            Message rocketMsg = convertToRocketMsg(topic, eventCode, key, payload);
            SendResult sendResult;
            if (Objects.nonNull(delayTimeLevel)) {
                rocketMsg.setDelayTimeLevel(delayTimeLevel);
            }
            if (hashBy == null) {
                sendResult = producer.send(rocketMsg, SELECT_QUEUE_BY_RANDOM, null, timeoutMs);
            } else {
                sendResult = producer.send(rocketMsg, SELECT_QUEUE_BY_HASH, hashBy, timeoutMs);
            }
            long costTime = System.currentTimeMillis() - now;
            log.debug("send message cost: {} ms, msgId:{}", costTime, sendResult.getMsgId());

            return handleSendResult(sendResult);
        } catch (Exception e) {
            log.info("syncSend failed. topic:{}, eventCode:{}, message:{} ,exception:{}",
                    topic, eventCode, payload, e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * 发送
     *
     * @param topic          topic
     * @param eventCode      eventCode
     * @param payload        消息体
     * @param timeoutMs      超时毫秒数
     * @return 发送结果
     */
    private void asyncSend(String topic, String eventCode, Object payload, long timeoutMs,SendCallback sendCallback) {
        ValuesUtil.checkTopicAndEventCode(topic, eventCode);
        try {
            long now = System.currentTimeMillis();
            Message rocketMsg = convertToRocketMsg(topic, eventCode,null, payload);
            producer.send(rocketMsg, sendCallback, timeoutMs);
            long costTime = System.currentTimeMillis() - now;
            log.debug("asyncSend message cost: {} ms", costTime);
        } catch (Exception e) {
            log.info("asyncSend failed. topic:{}, eventCode:{}, message:{} ,exception:{}",
                    topic, eventCode, payload, e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }


    /**
     * 发送（Oneway）
     *
     * @param topic          topic
     * @param eventCode      eventCode
     * @param payload        消息体
     * @param hashBy         根据hash值发送至特定queue
     * @param timeoutMs      超时毫秒数
     * @param delayLevelEnum 延时消息枚举
     */
    private void sendOneway(String topic, String eventCode, String key, Object payload, Object hashBy, long timeoutMs,
                            DelayLevelEnum delayLevelEnum) {
        ValuesUtil.checkTopicAndEventCode(topic, eventCode);
        try {
            long now = System.currentTimeMillis();
            Message rocketMsg = convertToRocketMsg(topic, eventCode, key, payload);
            if (Objects.nonNull(delayLevelEnum)) {
                rocketMsg.setDelayTimeLevel(delayLevelEnum.getCode());
            }
            if (hashBy == null) {
                producer.sendOneway(rocketMsg, SELECT_QUEUE_BY_RANDOM, null);
            } else {
                producer.sendOneway(rocketMsg, SELECT_QUEUE_BY_HASH, hashBy);
            }
            long costTime = System.currentTimeMillis() - now;
            if (log.isDebugEnabled()) {
                log.debug("send oneway message cost: {} ms", costTime);
            }
        } catch (Exception e) {
            log.info("sendOneway failed. topic:{}, eventCode:{}, message:{} ,exception:{}",
                    topic, eventCode, payload, e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }


    /**
     * 组装返回结果BaseResponse
     *
     * @param sendResult 发送结果
     * @return BaseResponse<Object>
     */
    private BaseResponse<Object> handleSendResult(SendResult sendResult) {
        BaseResponse<Object> result = new BaseResponse<>();
        if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
            result.success(sendResult);
        } else {
            result.setSuccess(false);
            result.setModel(sendResult);
            result.setErrorMessage(sendResult.getSendStatus().name());
        }
        return result;
    }
}
