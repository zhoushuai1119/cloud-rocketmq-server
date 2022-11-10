package com.cloud.platform.rocketmq.metrics;

import org.apache.rocketmq.client.producer.SendStatus;

/**
 * 生产端Metrics
 *
 * @Author Wang Lin(王霖)
 * @Date 2018/8/20
 * @Time 上午11:31
 */
public interface ProducerTimingSampleContext {


    void record(SendStatus sendStatus, Throwable throwable);
}
