package com.cloud.platform.rocketmq.metrics.impl;

import com.cloud.platform.common.constants.PlatformCommonConstant;
import com.cloud.platform.rocketmq.metrics.ConsumerTimingSampleContext;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: 消费端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 14:03
 */
public class ConsumerTimingSampleContextImpl implements ConsumerTimingSampleContext {

    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");
    public static final String CONSUME_METRICS_NAME = "mq.consumer";
    private static final String TIMEDTASK_LONG_TIME_METRICS_NAME = "mq.timed.task.longtime";
    private static final String TIMEDTASK_METRICS_NAME = "mq.timed.task";

    private final Timer.Sample timerSample;
    private final LongTaskTimer.Sample longTaskTimerSample;

    private MeterRegistry registry;
    private String topic;
    private String eventCode;
    private boolean retry;


    /**
     * 取样
     *
     * @param topic
     * @param eventCode
     */
    public ConsumerTimingSampleContextImpl(String topic, String eventCode, int reconsumeTimes, MeterRegistry registry) {
        this.topic = topic;
        this.eventCode = eventCode;
        this.registry = registry;
        this.retry = reconsumeTimes > 0;

        if (Objects.equals(PlatformCommonConstant.ScheduledJobTopic.SCHEDULED_JOB_TOPIC, topic)) {
            //定时任务
            this.longTaskTimerSample = LongTaskTimer.builder(TIMEDTASK_LONG_TIME_METRICS_NAME)
                    .tag("eventCode", eventCode)
                    .register(registry)
                    .start();
        } else {
            this.longTaskTimerSample = null;
        }
        this.timerSample = Timer.start(registry);
    }

    public void record(Throwable throwable) {
        try {
            this.timerSample.stop(Timer.builder(getMeterName())
                    .tags(generateTags(throwable))
                    .register(registry));

            if (this.longTaskTimerSample != null) {
                this.longTaskTimerSample.stop();
            }
        } catch (Throwable e) {
            //ignore
        }
    }

    private Tag exception(@Nullable Throwable exception) {
        return exception == null ? EXCEPTION_NONE : Tag.of("exception", exception.getClass().getSimpleName());
    }

    /**
     * 获取消费者指标名称
     *
     * @return
     */
    private String getMeterName() {
        if (Objects.equals(PlatformCommonConstant.ScheduledJobTopic.SCHEDULED_JOB_TOPIC, topic)) {
            return TIMEDTASK_METRICS_NAME;
        }
        return CONSUME_METRICS_NAME;
    }

    /**
     * 生成tags
     *
     * @param throwable
     * @return
     */
    private List<Tag> generateTags(@Nullable Throwable throwable) {
        if (PlatformCommonConstant.ScheduledJobTopic.SCHEDULED_JOB_TOPIC.equals(topic)) {
            return Arrays.asList(Tag.of("eventCode", eventCode), exception(throwable));
        } else {
            return Arrays.asList(Tag.of("key", topic + "#" + eventCode), Tag.of("retry", retry ? "true" : "false"), exception(throwable));
        }
    }
}
