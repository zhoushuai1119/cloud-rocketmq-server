package com.cloud.platform.rocketmq.core;

/**
 * Constants Created by shuai.zhou on 2018/11/16.
 */
public final class DefaultRocketMQListenerContainerConstants {
    public static final String PROP_NAMESERVER = "nameServer";
    public static final String PROP_TOPIC_MAP = "subscription";
    public static final String PROP_CONSUMER_GROUP = "consumerGroup";
    public static final String PROP_ENABLE_MSG_TRACE = "enableMsgTrace";
    public static final String PROP_CONSUME_MODE = "consumeMode";
    public static final String PROP_CONSUME_THREAD_MAX = "consumeThreadMax";
    public static final String PROP_CONSUME_THREAD_MIN = "consumeThreadMin";
    public static final String PROP_MESSAGE_MODEL = "messageModel";
    public static final String PROP_SELECTOR_TYPE = "selectorType";
    public static final String PROP_ROCKETMQ_LISTENER = "rocketMQListener";
    public static final String PROP_TIME_BASED_JOB_EXECUTOR = "timeBaseJobExecutor";
    public static final String PROD_DISCARD_TASK_SECONDS = "discardTaskSeconds";
    public static final String PROP_ROCKETMQ_TEMPLATE = "rocketMQTemplate";
    public static final String PROP_METRICS_PROPERTY = "metricsProperty";
    public static final String PROP_METRICS = "metrics";
    public static final String PROP_CONSUME_MESSAGE_BATCH_MAX_SIZE = "consumeMessageBatchMaxSize";
    public static final String METHOD_DESTROY = "destroy";
    public static final String TOPIC_THREAD_POOL_MAP = "topicThreadPoolConfig";
    public static final String THREAD_POOL_METER_REGISTRY = "threadPoolMeterRegistry";
    public static final String RPC_HOOK = "rpcHook";
}
