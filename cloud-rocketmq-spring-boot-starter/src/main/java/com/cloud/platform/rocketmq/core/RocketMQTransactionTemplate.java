package com.cloud.platform.rocketmq.core;

import com.cloud.mq.base.core.CloudTransactionMQTemplate;
import com.cloud.platform.rocketmq.enums.TransactionStateEnum;
import com.cloud.mq.base.utils.ValuesUtil;
import com.cloud.platform.common.response.BaseResponse;
import com.cloud.platform.rocketmq.utils.MqMessageUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * rocketMQ发送事务消息模板
 *
 */
@Slf4j
public class RocketMQTransactionTemplate implements CloudTransactionMQTemplate, InitializingBean, DisposableBean {

    @Getter
    @Setter
    private TransactionMQProducer producer;

    @Getter
    @Setter
    private String charset = "UTF-8";


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
     * 发送
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, Object payload) {
        return sendImpl(topic, eventCode, null, payload, (Object) null);
    }

    /**
     * 同步发送,支持key 和自定义业务参数
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param key       key
     * @param payload   消息体
     * @param arg       Custom business parameter
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, String key, Object payload, Object arg) {
        return sendImpl(topic, eventCode, key, payload, arg);
    }

    /**
     * 同步发送 支持自定义参数
     *
     * @param topic     topic
     * @param eventCode eventCode
     * @param payload   消息体
     * @param arg       Custom business parameter
     * @return 发送结果
     */
    @Override
    public BaseResponse<Object> send(String topic, String eventCode, Object payload, Object arg) {
        return sendImpl(topic, eventCode, null, payload, arg);
    }

    private BaseResponse<Object> sendImpl(String topic, String eventCode, String key, Object payload, Object arg) {
        ValuesUtil.checkTopicAndEventCode(topic, eventCode);
        try {
            long now = System.currentTimeMillis();
            Message rocketMsg = MqMessageUtil.convertToRocketMsg(topic, eventCode, key, payload);
            //如果不使用 sendMessageInTransaction 方法，当做普通消息发送
            TransactionSendResult sendResult = producer.sendMessageInTransaction(rocketMsg, arg);
            long costTime = System.currentTimeMillis() - now;
            log.debug("send message cost: {} ms, msgId:{}", costTime, sendResult.getMsgId());

            BaseResponse<Object> result = new BaseResponse<>();
            //失败
            if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                result.setSuccess(false);
                result.setModel(sendResult);
                result.setErrorMessage(sendResult.getSendStatus().name());

            } else if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())
                    && sendResult.getLocalTransactionState().name().equals(TransactionStateEnum.COMMIT_MESSAGE.name())) {
                //发送状态成功，但是事务消息状态为 COMMIT_MESSAGE 表示成功)
                result.success(sendResult);
            } else {
                result.setSuccess(false);
                result.setModel(sendResult);
            }

            return result;
        } catch (Exception e) {
            log.info("syncSend failed. topic:{}, eventCode:{}, message:{} ", topic, eventCode, payload);
            throw new MessagingException(e.getMessage(), e);
        }
    }
}
