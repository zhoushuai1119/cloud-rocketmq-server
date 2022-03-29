package com.cloud.platform.rocketmq.utils;

import com.cloud.mq.base.core.CloudMQListener;
import com.cloud.mq.base.dto.CloudMessage;
import com.cloud.platform.common.utils.JsonUtil;
import com.cloud.platform.rocketmq.core.TopicListener;
import com.cloud.platform.rocketmq.core.TopicTransactionListener;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @Author 马腾飞
 * @Date 2019/11/15
 * @Time 16:52
 * @Description
 */
@Slf4j
public class MqMessageUtil {

    private static String charset = "UTF-8";

    /**
     * [事务消息] 根据 Message类型转化消息 【事务消息本地事务和回查事务】
     * 消息体转化为String 在转化为对象
     */
    @SuppressWarnings("unchecked")
    public static CloudMessage<Object> doConvertMessageByClass(Message message, JavaType type) throws IOException {
        try {
            CloudMessage<Object> monMessage = new CloudMessage<>();
            monMessage.setTopic(message.getTopic());
            monMessage.setEventCode(message.getTags());
            monMessage.setTransactionId(message.getTransactionId());
            monMessage.setKey(message.getKeys());
            byte[] body = message.getBody();
            String str = new String(body, Charset.forName(charset));

            if (Objects.equals(String.class, type.getRawClass())) {
                monMessage.setPayload(str);
            } else {
                monMessage.setPayload(JsonUtil.OBJECT_MAPPER.readValue(str, type));
            }
            return monMessage;
        } catch (Exception e) {
            log.error("convert message error", e);
            throw e;
        }
    }

    /**
     * [接收消息] 根据 MessageExt 类型转化消息
     */
    @SuppressWarnings("unchecked")
    public static CloudMessage<Object> doConvertMessageExtByClass(MessageExt messageExt, Class messageType, boolean handlePayload) {
        CloudMessage<Object> monMessage = new CloudMessage<>();
        monMessage.setTopic(messageExt.getTopic());
        monMessage.setEventCode(messageExt.getTags());
        monMessage.setKey(messageExt.getKeys());
        monMessage.setMessageId(messageExt.getMsgId());
        monMessage.setCreateTimeStamp(messageExt.getBornTimestamp());
        monMessage.setReconsumeTimes(messageExt.getReconsumeTimes());

        byte[] bodyExt = messageExt.getBody();

        if (!handlePayload) {
            return monMessage;
        }

        if (Objects.equals(messageType, MessageExt.class)) {
            monMessage.setPayload(messageExt);
            return monMessage;
        }

        String str = new String(bodyExt, Charset.forName(charset));
        if (Objects.equals(messageType, String.class)) {
            monMessage.setPayload(str);
        } else {
            // if msgType not string, use objectMapper change it.
            try {
                monMessage.setPayload(JsonUtil.toBean(str, messageType));
            } catch (Exception e) {
                log.info("convert failed. str:{}, msgType:{}", str, messageType);
                throw new RuntimeException("cannot convert message to " + messageType, e);
            }
        }
        return monMessage;
    }

    /**
     * 转换消息内的 string -> object
     *
     * @param originalMessage
     * @param type
     * @return
     */
    public static CloudMessage<Object> convertMessage(CloudMessage<String> originalMessage, JavaType type) throws IOException {
        try {
            CloudMessage<Object> message = new CloudMessage<>();
            message.setMessageId(originalMessage.getMessageId());
            message.setTopic(originalMessage.getTopic());
            message.setEventCode(originalMessage.getEventCode());
            message.setKey(originalMessage.getKey());
            message.setCreateTimeStamp(originalMessage.getCreateTimeStamp());
            message.setReconsumeTimes(originalMessage.getReconsumeTimes());

            if (!StringUtils.isEmpty(originalMessage.getPayload())) {
                if (Objects.equals(String.class, type.getRawClass())) {
                    message.setPayload(originalMessage.getPayload());
                } else {
                    message.setPayload(JsonUtil.OBJECT_MAPPER.readValue(originalMessage.getPayload(), type));
                }
            }
            return message;
        } catch (Exception e) {
            log.error("convert message error", e);
            throw e;
        }
    }

    /**
     * 将消息转化为 rocketmq message 对象
     */
    public static Message convertToRocketMsg(String topic, String eventCode, String key, Object payLoad) {
        String payloadStr = JsonUtil.toString(payLoad);
        if (payloadStr == null) {
            payloadStr = "";
        }
        byte[] payloads = payloadStr.getBytes(Charset.forName(charset));

        Message rocketMsg = new Message(topic, eventCode, key, payloads);
        return rocketMsg;
    }


    /**
     * 获取TopicListener的泛型,(消费者使用)
     * topicInfo.setMessageType()启动调用
     *
     * @param topicListener
     * @return
     */
    public static JavaType getMessageJavaType2Topic(TopicListener topicListener) {
        Type[] interfaces = null;
        Class clazz = topicListener.getClass();
        if (AopUtils.isAopProxy(topicListener)) {
            //@Transactional会增加AOP代理，查找真实的类对象
            clazz = ((Advised) topicListener).getTargetSource().getTargetClass();
        }
        interfaces = clazz.getGenericInterfaces();

        if (Objects.nonNull(interfaces)) {
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    if (Objects.equals(parameterizedType.getRawType(), TopicListener.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
                            Type actualTypeArgu = actualTypeArguments[0];
                            return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(actualTypeArgu);

                        } else {
                            return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Object.class);
                        }
                    }
                }
            }

            return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Object.class);
        } else {
            return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Object.class);
        }
    }

    /**
     * 获取MonsterMQListener 的Class
     * 启动时候调用 this.messageType = MqMessageUtil.getMessageTypeByMonsterMQListener(rocketMQListener);
     */
    public static Class getMessageTypeByMonsterMQListener(CloudMQListener cloudMQListener) {
        Type[] interfaces = null;
        Class clazz = cloudMQListener.getClass();
        if (AopUtils.isAopProxy(cloudMQListener)) {
            //@Transactional会增加AOP代理，查找真实的类对象
            clazz = ((Advised) cloudMQListener).getTargetSource().getTargetClass();
        }
        interfaces = clazz.getGenericInterfaces();


        if (Objects.nonNull(interfaces)) {
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    if (Objects.equals(parameterizedType.getRawType(), CloudMQListener.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
                            return (Class) actualTypeArguments[0];
                        } else {
                            return Object.class;
                        }
                    }
                }
            }

            return Object.class;
        } else {
            return Object.class;
        }
    }

    /**
     * 获取TopicTransactionListener的泛型
     * topicInfo.setMessageType（）启动调用
     */
    public static JavaType getMessageJavaType2TopicTransaction(TopicTransactionListener topicListener) {
        Type type = topicListener.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
                Type actualTypeArgu = actualTypeArguments[0];
                return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(actualTypeArgu);
            } else {
                return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Object.class);
            }
        }

        return JsonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Object.class);

    }

}
