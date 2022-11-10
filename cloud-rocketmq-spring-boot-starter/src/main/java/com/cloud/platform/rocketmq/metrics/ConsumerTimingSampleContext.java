package com.cloud.platform.rocketmq.metrics;

/**
 * @description: 消费端Metrics
 * @author: zhou shuai
 * @date: 2022/11/10 13:59
 */
public interface ConsumerTimingSampleContext {

    void record(Throwable throwable);

}
