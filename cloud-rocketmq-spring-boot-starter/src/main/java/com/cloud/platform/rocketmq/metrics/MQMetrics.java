package com.cloud.platform.rocketmq.metrics;

import org.apache.rocketmq.client.producer.SendStatus;

import java.util.List;

/**
 * @description: 消费端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 13:58
 */
public interface MQMetrics {

    ProducerTimingSampleContext startProduce(String topic, String eventCode);

    ProducerTimingSampleContext startBatchProduce(String topic, List<String> eventCodes);

    /**
     * 记录生产者 Metrics 信息
     *
     * @param context
     * @param sendStatus
     * @param throwable
     */
    void recordProduce(ProducerTimingSampleContext context, SendStatus sendStatus, Throwable throwable);

    ConsumerTimingSampleContext startConsume(String topic, String eventCode, int reconsumeTimes);

    /**
     * 记录消费者 Metrics 信息
     *
     * @param context
     * @param throwable
     */
    void recordConsumer(ConsumerTimingSampleContext metricsContext, Throwable throwable);

}
