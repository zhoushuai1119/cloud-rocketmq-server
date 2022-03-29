package com.cloud.platform.rocketmq.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @description: 消息注解
 * @author: zhou shuai
 * @date: 2022/1/11 18:14
 * @version: v1
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ConsumeTopic {

    /**
     * topic
     *
     * @return
     */
    String topic();

    /**
     * eventCode
     *
     * @return
     */
    String eventCode();

    /**
     * 是否打印日志
     * @return
     */
    boolean log() default false;

}
