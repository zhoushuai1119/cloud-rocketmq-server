package com.cloud.platform.rocketmq.metrics.impl;

import com.cloud.platform.rocketmq.metrics.ProducerTimingSampleContext;
import com.google.common.collect.Maps;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.Nullable;
import org.apache.rocketmq.client.producer.SendStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @description: 生产端Metrics(批量消息)
 * @author: zhou shuai
 * @date: 2022/11/10 14:03
 */
public class BatchProducerTimingSampleContextImpl implements ProducerTimingSampleContext {

    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");
    private final Map<String, Timer.Sample> timerSamples = Maps.newHashMap();
    private MeterRegistry registry;
    private String topic;


    public BatchProducerTimingSampleContextImpl(String topic, List<String> eventCodes, MeterRegistry registry) {
        this.topic = topic;
        this.registry = registry;
        eventCodes.forEach(eventCode -> timerSamples.put(eventCode, Timer.start(registry)));
    }

    @Override
    public void record(SendStatus sendStatus, Throwable throwable) {
        try {
            timerSamples.forEach((eventCode, timerSample) ->
                    timerSample.stop(Timer.builder(ProducerTimingSampleContextImpl.PRODUCER_METRICS_NAME)
                            .tags(generateTags(this.topic, eventCode, sendStatus, throwable))
                            .register(registry)));
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
        return Arrays.asList(Tag.of("key", topic + "#" + eventCode), Tag.of("type", "batch"), Tag.of("status", status),
                exception(throwable));
    }
}
