package com.cloud.platform.rocketmq.utils;

import com.cloud.platform.rocketmq.RocketMQProperties;
import com.cloud.platform.rocketmq.metrics.ConsumerTimingSampleContext;
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
     * 返回生产者 Metrics
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
     * @param sendStatus      MQ发送结果状态
     * @param throwable       异常
     */
    public static void recordProduce(RocketMQProperties.Metrics metricsProperty, MQMetrics metrics,
                                     ProducerTimingSampleContext metricsContext, SendStatus sendStatus,
                                     Throwable throwable) {
        if (metricsProperty.isEnabled() && metrics != null) {
            metrics.recordProduce(metricsContext, sendStatus, throwable);
        }
    }

    /**
     * 返回消费者 Metrics
     *
     * @param metrics         metrics
     * @param metricsProperty metrics配置
     * @param topic           topic
     * @param eventCode       eventCode
     * @param reconsumeTimes  重复次数
     * @return ConsumerTimingSampleContext
     */
    public static ConsumerTimingSampleContext startConsumer(RocketMQProperties.Metrics metricsProperty, MQMetrics metrics,
                                                            String topic, String eventCode, int reconsumeTimes) {
        if (metricsProperty.isEnabled() && metrics != null) {
            return metrics.startConsume(topic, eventCode, reconsumeTimes);
        }
        return null;
    }


    /**
     * 记录消费情况
     *
     * @param metricsProperty metrics配置
     * @param metrics         metrics
     * @param metricsContext  metricsContext
     * @param sendStatus      MQ发送结果状态
     * @param throwable       异常
     */
    public static void recordConsumer(RocketMQProperties.Metrics metricsProperty, MQMetrics metrics,
                                      ConsumerTimingSampleContext metricsContext, Throwable throwable) {
        if (metricsProperty.isEnabled() && metrics != null) {
            metrics.recordConsumer(metricsContext, throwable);
        }
    }

}
