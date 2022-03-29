package com.cloud.mq.base.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.protocol.NamespaceUtil;

import java.util.List;

/**
 * 批量消息工具类
 *
 * @author LiWenJu(李文举)
 * @date 2020/11/4
 */
@Slf4j
public class BatchMsgUtil {


    /**
     * 装配MessageBatch
     * 逻辑同DefaultMQProducer&batch()
     * @param messageList List<Message>
     * @param producer    DefaultMQProducer
     * @return MessageBatch
     * @throws MQClientException
     */
    public static MessageBatch batch(List<Message> messageList, DefaultMQProducer producer) throws MQClientException {
        MessageBatch msgBatch;
        try {
            msgBatch = MessageBatch.generateFromList(messageList);
            for (Message message : msgBatch) {
                Validators.checkMessage(message, producer);
                MessageClientIDSetter.setUniqID(message);
                message.setTopic(NamespaceUtil.wrapNamespace(producer.getNamespace(), message.getTopic()));
            }
            msgBatch.setBody(msgBatch.encode());
        } catch (Exception e) {
            throw new MQClientException("Failed to initiate the MessageBatch", e);
        }
        msgBatch.setTopic(producer.withNamespace(msgBatch.getTopic()));
        return msgBatch;
    }
}
