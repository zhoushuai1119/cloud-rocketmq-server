package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.dto.CloudMessage;
import com.cloud.platform.common.constants.PlatformCommonConstant;
import com.cloud.platform.rocketmq.annotation.TansactionTopic;
import com.cloud.platform.rocketmq.utils.MqMessageUtil;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.aop.framework.AopProxyUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 现类用于接收 mq原始 封装
 *
 * @author shuai.zhou
 */
@Slf4j
public class DefaultTopicTransactionListenerImpl implements TransactionListener {

    /**
     * key 为 topicAndEventCode
     * ConsumeTransTopicInfo 有对应的消费逻辑 listener
     */
    protected static Map<String, TransactionTopicMsgInfo> topicConsumerMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private String charset = "UTF-8";

    /**
     * 执行本地事务
     */
    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
        String key = message.getTopic() + ":" + message.getTags();
        TransactionTopicMsgInfo transTopicInfo = topicConsumerMap.get(key);
        if (Objects.nonNull(transTopicInfo)) {
            CloudMessage<Object> messageConvert = null;
            try {
                messageConvert = MqMessageUtil.doConvertMessageByClass(message, transTopicInfo.getMessageType());
            } catch (Exception e) {
                return LocalTransactionState.UNKNOW;
            }

            LocalTransactionState transactionStateEnum = transTopicInfo.getTopicTransactionListener()
                    .executeTransaction(messageConvert, arg);
            return transactionStateEnum;
        } else {
            log.warn("cant find local transaction listener key = {}", key);
        }
        return LocalTransactionState.UNKNOW;
    }

    /**
     * 回查事务
     */
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt message) {
        String key = message.getTopic() + ":" + message.getTags();
        TransactionTopicMsgInfo transTopicInfo = topicConsumerMap.get(key);
        if (Objects.nonNull(transTopicInfo)) {
            CloudMessage<Object> messageConvert = null;
            try {
                messageConvert = MqMessageUtil.doConvertMessageByClass(message, transTopicInfo.getMessageType());
            } catch (Exception e) {
                return LocalTransactionState.UNKNOW;
            }
            LocalTransactionState transactionStateEnum = transTopicInfo.getTopicTransactionListener().checkLocalTransaction(messageConvert);
            return transactionStateEnum;
        } else {
            log.warn("cant find check transaction listener key = {}", key);
        }
        return LocalTransactionState.UNKNOW;
    }

    /**
     * 注册监听到map内
     *
     * @author shuai.zhou
     * @Date 2019/12/11 15:58
     */
    protected static void registerListener(TopicTransactionListener topicListener) throws Exception {
        //listener 注解中的 topicAndEventCode 得到 topicInfo
        TransactionTopicMsgInfo topicInfo = getConsumeTopicAndEventCode(topicListener);
        if (topicInfo != null) {
            String topicAndEventCode = topicInfo.getTopic() + ":" + topicInfo.getEventCode();
            TransactionTopicMsgInfo existed = topicConsumerMap.get(topicAndEventCode);
            if (existed != null) {
                throw new Exception("duplicated topic eventCode listener: "
                        + existed.getTopicTransactionListener().getClass().getName() + " and " + topicListener.getClass().getName());
            } else {
                topicInfo.setTopicTransactionListener(topicListener);
                //设置消息转化类型
                topicInfo.setMessageType(MqMessageUtil.getMessageJavaType2TopicTransaction(topicInfo.getTopicTransactionListener()));
                topicConsumerMap.put(topicAndEventCode, topicInfo);
            }
        }
    }

    /**
     * 从ConsumeTopic注解中获取topic和eventCode
     *
     * @param consumeTopic
     * @return
     */
    private static TransactionTopicMsgInfo getConsumeTopicAndEventCode(TopicTransactionListener consumeTopic) throws Exception {
        //获取代理对象的原始类型(因为添加@Transactional会生成aop代理对象)
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(consumeTopic);
        TansactionTopic annotation = clazz.getAnnotation(TansactionTopic.class);
        if (Objects.isNull(annotation)) {
            log.error("transaction topic listener {} has no consumetopic annotation", consumeTopic.getClass().getName());
            return null;
        }
        if (Objects.equals(PlatformCommonConstant.SymbolParam.ALL_SYMBOL, annotation.eventCode().trim())) {
            throw new Exception("TansactionTopic eventCode must not use * , please use specific code");
        }
        TransactionTopicMsgInfo topicInfo = new TransactionTopicMsgInfo();
        topicInfo.setTopic(annotation.topic());
        topicInfo.setEventCode(annotation.eventCode());
        return topicInfo;
    }

    /**
     * topic事务信息
     */
    @Data
    private static final class TransactionTopicMsgInfo {
        /**
         * topic
         */
        private String topic;

        /**
         * eventCode
         */
        private String eventCode;

        /**
         * listener
         */
        private TopicTransactionListener topicTransactionListener;

        /**
         * 消息的json转换类型
         */
        private JavaType messageType;
    }

}
