package com.cloud.platform.rocketmq.utils;

import com.cloud.platform.rocketmq.RocketMQProperties;
import com.cloud.platform.rocketmq.metrics.MQMetrics;
import com.cloud.platform.rocketmq.metrics.ProducerTimingSampleContext;
import org.apache.rocketmq.client.producer.SendStatus;

/**
 * @description: metrics工具类
 * @author: zhou shuai
 * @date: 2022/11/10 19:28
 * @version: v1
 */
public class MetricsUtil {

    /**
     * 开始消费监控
     *
     * @param metricsProperty metrics配置
     * @param metrics         metrics
     * @param topic           topic
     * @param eventCode       eventCode
     * @return ProducerTimingSampleContext
     */
    public static ProducerTimingSampleContext startProduce(RocketMQProperties.Metrics metricsProperty, MQMetrics metrics,
                                                           String topic, String eventCode) {
        if (metricsProperty.isEnabled() && metrics != null) {
            return metrics.startProduce(topic, eventCode);
        }
        return null;
    }

    /**
     * 记录生产情况
     *
     * @param metricsProperty metrics配置
     * @param metrics         metrics
     * @param metricsContext  metricsContext
     * @param sendStatus      发送结果状态
     * @param sentBytes       发送字节数
     * @param throwable       异常
     */
    public static void recordProduce(RocketMQProperties.Metrics metricsProperty, MQMetrics metrics,
                                     ProducerTimingSampleContext metricsContext, SendStatus sendStatus,
                                     long sentBytes, Throwable throwable) {
        if (metricsProperty.isEnabled() && metrics != null) {
            metrics.recordProduce(metricsContext, sendStatus, sentBytes, throwable);
        }
    }

}
