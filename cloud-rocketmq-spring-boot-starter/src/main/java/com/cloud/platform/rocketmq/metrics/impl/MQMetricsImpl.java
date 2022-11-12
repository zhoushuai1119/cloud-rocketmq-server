package com.cloud.platform.rocketmq.metrics.impl;

import com.cloud.platform.common.constants.PlatformCommonConstant;
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
    private Counter globalProduceCount;
    private Counter globalConsumerCount;

    public MQMetricsImpl(MeterRegistry registry) {
        this.registry = registry;

        globalProduceCount = Counter.builder("mq.global.produce.count")
                .description("mq producer send total count")
                .register(registry);

        globalConsumerCount = Counter.builder("mq.global.consumer.count")
                .description("mq consumer total count")
                .register(registry);
    }

    @Override
    public ProducerTimingSampleContext startProduce(String topic, String eventCode) {
        //定时任务反馈不计入监控
        if (PlatformCommonConstant.FeedBackTopic.FEEDBACK_TASK_TOPIC.equals(topic)) {
            return null;
        }
        try {
            return new ProducerTimingSampleContextImpl(topic, eventCode, registry);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public ProducerTimingSampleContext startBatchProduce(String topic, List<String> eventCodes) {
        //定时任务反馈不计入监控
        if (PlatformCommonConstant.FeedBackTopic.FEEDBACK_TASK_TOPIC.equals(topic)) {
            return null;
        }
        try {
            return new BatchProducerTimingSampleContextImpl(topic, eventCodes, registry);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 记录生产者 Metrics 信息
     *
     * @param context
     * @param sendStatus
     * @param throwable
     */
    @Override
    public void recordProduce(ProducerTimingSampleContext context, SendStatus sendStatus, Throwable throwable) {
        try {
            if (context != null) {
                context.record(sendStatus, throwable);
                if (globalProduceCount != null) {
                    globalProduceCount.increment();
                }
            }
        } catch (Throwable e) {
            //ignore
        }
    }

    @Override
    public ConsumerTimingSampleContext startConsume(String topic, String eventCode, int reconsumeTimes) {
        try {
            return new ConsumerTimingSampleContextImpl(topic, eventCode, reconsumeTimes, registry);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 记录消费者 Metrics 信息
     *
     * @param context
     * @param throwable
     */
    @Override
    public void recordConsumer(ConsumerTimingSampleContext context, Throwable throwable) {
        try {
            if (context != null) {
                context.record(throwable);
                if (globalConsumerCount != null) {
                    globalConsumerCount.increment();
                }
            }
        } catch (Throwable e) {
            //ignore
        }
    }

}
