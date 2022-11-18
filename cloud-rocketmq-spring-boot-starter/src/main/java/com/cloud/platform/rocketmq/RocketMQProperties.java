package com.cloud.platform.rocketmq;

import lombok.Data;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rocketMQ配置
 */
@Data
@ConfigurationProperties(prefix = "cloud.rocketmq")
public class RocketMQProperties {

    /**
     * nameServer.
     * 格式: `host1:port;host2:port`
     */
    private String nameServer;

    /**
     * 生产者配置
     */
    private Producer producer;

    /**
     * 事务消息生产者配置
     */
    private TransactionProducerCustom transactionProducerCustom;

    /**
     * 消费者配置.
     * key: consumer_group,需要和spring bean 同名；value：consumer配置信息
     */
    private Consumer consumer;

    /**
     * 监控
     */
    private Metrics metrics = new Metrics();


    @Data
    public static class Producer {

        /**
         * 生产者组名称
         */
        private String groupName;

        /**
         * 超时毫秒数.
         * 默认3秒
         */
        private int sendMsgTimeout = 3000;

        /**
         * 消息体压缩阈值.
         * 对超过阈值的消息进行压缩，默认4K Byte
         */
        private int compressMsgBodyOverHowmuch = 1024 * 4;

        /**
         * 同步发送失败重试次数.
         * 默认为2，表示发送失败后最多再重试2次，总共最多发送3次。
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * 异步发送重试次数.
         * 暂时未用到异步发送
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * 当发送到broker后，broker存储失败是否重试其他broker.
         */
        private boolean retryAnotherBrokerWhenNotStoreOk = true;

        /**
         * 是否开启消息轨迹
         */
        private boolean enableMsgTrace = false;

        /**
         * The name value of message trace topic.If you don't config,you can use the default trace topic name.
         */
        private String customizedTraceTopic = TopicValidator.RMQ_SYS_TRACE_TOPIC;

        /**
         * 消息体最大2M.
         */
        private int maxMessageSize = 1024 * 1024 * 2;

    }

    @Data
    public static class TransactionProducerCustom extends Producer {
        /**
         * 检查事务线程数
         */
        private TransactionExecutorConf checkThreadPool;

    }

    @Data
    public static class Consumer {
        /**
         * 消费者组名称
         */
        private String groupName;

        /**
         * 是否是广播模式.
         * 默认false 点对点模式，相同消费组只有一个节点获得消息
         */
        private boolean broadcasting = false;

        /**
         * Min consumer thread number
         */
        private int consumeThreadMin = 20;

        /**
         * Max consumer thread number
         */
        private int consumeThreadMax = 100;

        /**
         * 消费失败后重试次数.
         * 重试间隔分别为 第一次：1s 第二次：5s 第三次：10s 第四次：30s 第五/六/...次：1m
         */
        private int retryTimesWhenConsumeFailed = 3;

        /**
         * 过滤消息  支持 SQL92 TAG
         * consumer.subscribe("TOPIC", "TAGA || TAGB || TAGC");
         * consumer.subscribe("TopicTest", MessageSelector.bySql("a between 0 and 3");
         */
        private String SelectorType = "TAG";

        /**
         * 消费顺序,默认为false,采用并发消费
         */
        private boolean orderly = false;

        /**
         * Batch consumption size
         */
        private int consumeMessageBatchMaxSize = 1;

        /**
         * 是否开启消息轨迹，默认为false
         */
        private boolean enableMsgTrace = false;

    }

    @Data
    public static class Metrics {

        /**
         * 是否开启metrics. 默认开启
         */
        private boolean enabled = true;

    }


    @Data
    public static class TransactionExecutorConf {

        private int corePoolSize = 5;

        private int maximumPoolSize = 10;

        private int keepAliveTime = 200;

    }

}
