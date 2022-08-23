package com.cloud.platform.rocketmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * rocketMQ配置
 */
@Data
@ConfigurationProperties(prefix = "cloud.rocketmq")
public class RocketMQProperties {

    /**
     * nameServer.
     * 格式: `host:port;host:port`
     */
    private String nameServer;

    /**
     * 生产者配置
     */
    private Producer producer;

    /**
     * 校验topic eventCode 关系是否匹配
     */
    private Boolean topicRelationCheck = true;

    /**
     * 事务消息生产者配置
     */
    private TransactionProducerCustom transactionProducerCustom;

    /**
     * 消费者配置.
     * key: consumer_group,需要和spring bean 同名；value：consumer配置信息
     */
    private Map<String, Consumer> consumer;


    @Data
    public static class Producer {

        /**
         * 生产者group
         */
        private String group;

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
         * 消息体最大2M.
         */
        private int maxMessageSize = 1024 * 1024 * 2; // 2M

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
         * 消费的topicMap.
         * key: topic, value:eventCodes format:{ec_0,ec_1}
         */
        private Map<String, String> subscription;

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
         * 是否开启消息轨迹
         */
        private boolean enableMsgTrace = false;

    }


    @Data
    public static class TransactionExecutorConf {
        private int corePoolSize = 5;

        private int maximumPoolSize = 10;

        private int keepAliveTime = 200;

    }

}
