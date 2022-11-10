package com.cloud.platform.rocketmq.metrics.impl;

import com.cloud.platform.rocketmq.metrics.ConsumerTimingSampleContext;
import com.cloud.platform.rocketmq.metrics.MQMetrics;
import com.cloud.platform.rocketmq.metrics.ProducerTimingSampleContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.rocketmq.client.producer.SendStatus;

import java.util.List;

/**
 * @description: 消费端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 14:04
 */
public class MQMetricsImpl implements MQMetrics {
    private MeterRegistry registry;
    private Counter globalProduceBytes;
    private Counter globalProduceCount;

    public MQMetricsImpl(MeterRegistry registry) {
        this.registry = registry;

        globalProduceBytes = Counter.builder("mq.global.produce")
                .baseUnit("bytes")
                .description("mq producer send total bytes")
                .register(registry);
        globalProduceCount = Counter.builder("mq.global.produce.count")
                .description("mq producer send total count")
                .register(registry);
    }

    @Override
    public ConsumerTimingSampleContext startConsume(String topic, String eventCode, int reconsumeTimes) {
        try {
            return new ConsumerTimingSampleContextImpl(topic, eventCode, reconsumeTimes, registry);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ProducerTimingSampleContext startProduce(String topic, String eventCode) {
        if (ProducerTimingSampleContextImpl.TOPIC_TIME_TASK_FEEDBACK.equals(topic)) {
            //定时任务反馈不计入监控
            return null;
        }
        try {
            return new ProducerTimingSampleContextImpl(topic, eventCode, registry);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ProducerTimingSampleContext startBatchProduce(String topic, List<String> eventCodes) {
        if (ProducerTimingSampleContextImpl.TOPIC_TIME_TASK_FEEDBACK.equals(topic)) {
            //定时任务反馈不计入监控
            return null;
        }
        try {
            return new BatchProducerTimingSampleContextImpl(topic, eventCodes, registry);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void recordProduce(ProducerTimingSampleContext context, SendStatus sendStatus, long totalBytes, Throwable throwable) {
        try {
            if (context != null) {
                context.record(sendStatus, throwable);
                if (globalProduceBytes != null) {
                    globalProduceBytes.increment(totalBytes);
                }
                if (globalProduceCount != null) {
                    globalProduceCount.increment();
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }
}
