package com.cloud.platform.rocketmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 定时任务配置
 * @author: zhoushuai
 */
@ConfigurationProperties(prefix = "cloud.time-based-job")
@Data
public class TimeBasedJobProperties {

    /**
     * 定时任务订阅topic.
     */
    public static final String JOB_TOPIC = "TP_F_SC";

    /**
     * 定时任务反馈 topic
     * 客户端完成任务后发送topic给定时任务服务器，反馈执行结果.
     */
    public static final String JOB_TOPIC_FEEDBACK = "TP_F_FB";

    /**
     * 定时任务反馈 eventCode
     * 客户端完成任务后发送topic给定时任务服务器，反馈执行结果.
     */
    public static final String JOB_EVENTCODE_FEEDBACK = "EC_RESULT";

    /**
     * 异步处理任务的线程数量.
     * 默认20
     */
    private int threadPoolSize = 20;

    /**
     * 发送时间，抛弃定时任务的秒数, 默认5分钟。
     * RocketMQ，历史消息如果未消费，应用启动后会继续消费，不适用于定时任务的场景；
     * 定义该参数，发送时间超过discardTaskSeconds的定时任务将被抛弃。
     */
    private int discardTaskSeconds = 300;

    /**
     * 是否启用.
     */
    private boolean enabled = false;

}
