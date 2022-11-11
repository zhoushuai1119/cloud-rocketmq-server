package com.cloud.platform.rocketmq.metrics.impl;

import com.cloud.platform.rocketmq.metrics.ProducerTimingSampleContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.Nullable;
import org.apache.rocketmq.client.producer.SendStatus;

import java.util.Arrays;
import java.util.List;

/**
 * @description: 生产端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 20:10
 */
public class ProducerTimingSampleContextImpl implements ProducerTimingSampleContext {

    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");
    public static final String PRODUCER_METRICS_NAME = "mq.producer";
    public static final String TOPIC_TIME_TASK_FEEDBACK = "TP_F_FB";
    private final Timer.Sample timerSample;
    private MeterRegistry registry;
    private String topic;
    private String eventCode;


    /**
     * 取样
     *
     * @param topic
     * @param eventCode
     */
    public ProducerTimingSampleContextImpl(String topic, String eventCode, MeterRegistry registry) {
        this.topic = topic;
        this.eventCode = eventCode;
        this.registry = registry;
        this.timerSample = Timer.start(registry);
    }

    @Override
    public void record(SendStatus sendStatus, Throwable throwable) {
        try {
            this.timerSample.stop(Timer.builder(PRODUCER_METRICS_NAME)
                    .tags(generateTags(this.topic, this.eventCode, sendStatus, throwable))
                    .register(registry));
        } catch (Exception e) {
            //ignore
        }
    }

    private Tag exception(@Nullable Throwable exception) {
        return exception == null ? EXCEPTION_NONE : Tag.of("exception", exception.getClass().getSimpleName());
    }

    /**
     * 生成tags
     *
     * @param topic
     * @param eventCode
     * @param throwable
     * @return
     */
    private List<Tag> generateTags(String topic, String eventCode, SendStatus sendStatus, Throwable throwable) {
        String status = null;
        if (sendStatus != null) {
            status = sendStatus.name();
        } else if (throwable != null) {
            status = "FAILURE";
        } else {
            status = "UNKNOWN";
        }
        return Arrays.asList(Tag.of("key", topic + "#" + eventCode), Tag.of("status", status), exception(throwable));
    }
}
