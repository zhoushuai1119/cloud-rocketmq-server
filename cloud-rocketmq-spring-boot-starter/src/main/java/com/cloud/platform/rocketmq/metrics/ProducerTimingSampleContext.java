package com.cloud.platform.rocketmq.metrics;

import org.apache.rocketmq.client.producer.SendStatus;

/**
 * 生产端Metrics
 * @author zhoushuai
 */
public interface ProducerTimingSampleContext {
    void record(SendStatus sendStatus, Throwable throwable);
}
