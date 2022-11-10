package com.cloud.platform.rocketmq.metrics;

import org.apache.rocketmq.client.producer.SendStatus;

import java.util.List;

/**
 * @description: 消费端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 13:58
 */
public interface MQMetrics {

    ConsumerTimingSampleContext startConsume(String topic, String eventCode, int reconsumeTimes);

    ProducerTimingSampleContext startProduce(String topic, String eventCode);

    ProducerTimingSampleContext startBatchProduce(String topic, List<String> eventCodes);

    void recordProduce(ProducerTimingSampleContext context, SendStatus sendStatus, long totalBytes, Throwable throwable);

}
