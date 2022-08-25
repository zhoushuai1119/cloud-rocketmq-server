package com.cloud.platform.rocketmq.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 事务消息 topic eventcode
 *
 * @author shuai.zhou
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TansactionTopic {

    /**
     * topic
     */
    String topic();

    /**
     * eventCode
     */
    String eventCode();

}
