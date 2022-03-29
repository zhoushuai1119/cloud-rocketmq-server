package com.cloud.mq.base.utils;

import org.springframework.util.Assert;

/**
 * 值工具
 *
 * @author LiWenJu(李文举)
 * @date 2020/11/4
 */
public class ValuesUtil {


    /**
     * 带默认值的取值方法
     *
     * @param value        提供值
     * @param defaultValue 默认值
     * @param <T>          T
     * @return T
     */
    public static <T> T nullOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }


    /**
     * 校验topic&eventCode
     *
     * @param topic     topic
     * @param eventCode eventCode
     */
    public static void checkTopicAndEventCode(String topic, String eventCode) {
        checkTopic(topic);
        checkEventCode(eventCode);
    }

    /**
     * 校验topic
     *
     * @param topic topic
     */
    public static void checkTopic(String topic) {
        Assert.hasText(topic, "'topic' is required");
    }

    /**
     * 校验eventCode
     *
     * @param eventCode eventCode
     */
    public static void checkEventCode(String eventCode) {
        Assert.hasText(eventCode, "'eventCode' is required");
    }
}
