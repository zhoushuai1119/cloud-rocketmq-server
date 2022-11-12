package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.core.CloudMQListener;
import com.cloud.mq.base.dto.PushMessage;
import com.cloud.platform.common.utils.JsonUtil;
import com.cloud.platform.rocketmq.RocketMQProperties;
import com.cloud.platform.rocketmq.TimeBasedJobProperties;
import com.cloud.platform.rocketmq.enums.ConsumeMode;
import com.cloud.platform.rocketmq.enums.SelectorType;
import com.cloud.platform.rocketmq.exception.DiscardOldJobException;
import com.cloud.platform.rocketmq.metrics.ConsumerTimingSampleContext;
import com.cloud.platform.rocketmq.metrics.MQMetrics;
import com.cloud.platform.rocketmq.timedjob.TimeBasedJobFeedback;
import com.cloud.platform.rocketmq.timedjob.TimeBasedJobMessage;
import com.cloud.platform.rocketmq.utils.MetricsUtil;
import com.cloud.platform.rocketmq.utils.MqMessageUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragelyByCircle;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author shuai.zhou
 */
@Slf4j
public class DefaultRocketMQListenerContainer implements InitializingBean, DisposableBean {

    @Setter
    @Getter
    private long suspendCurrentQueueTimeMillis = 1000;

    /**
     * Message consume retry strategy<br> -1,no retry,put into DLQ directly<br> 0,broker control retry frequency<br>
     * >0,client control retry frequency
     */
    @Setter
    @Getter
    private int delayLevelWhenNextConsume = 0;

    @Setter
    @Getter
    private String consumerGroup;

    @Setter
    @Getter
    private Boolean enableMsgTrace;

    @Setter
    @Getter
    private String nameServer;
    /**
     * key: topic, value: tags
     */
    @Setter
    @Getter
    private Map<String, String> subscription;

    @Setter
    @Getter
    private ConsumeMode consumeMode = ConsumeMode.CONCURRENTLY;

    @Setter
    @Getter
    private SelectorType selectorType = SelectorType.TAG;

    @Setter
    @Getter
    private MessageModel messageModel = MessageModel.CLUSTERING;

    @Setter
    @Getter
    private int consumeThreadMax = 64;

    @Setter
    @Getter
    private int consumeThreadMin = 20;

    @Getter
    @Setter
    private String charset = "UTF-8";

    @Setter
    @Getter
    private boolean started;

    @Setter
    private CloudMQListener rocketMQListener;

    @Setter
    private ExecutorService timeBaseJobExecutor;

    @Setter
    @Getter
    private int discardTaskSeconds;

    @Getter
    @Setter
    private MQMetrics metrics;

    @Getter
    @Setter
    private RocketMQProperties.Metrics metricsProperty;

    private AtomicLong discardedTaskCount = new AtomicLong();

    @Setter
    private RocketMQTemplate rocketMQTemplate;

    private DefaultMQPushConsumer consumer;

    @Setter
    private RPCHook rpcHook;

    private Class messageType;

    private static final Integer defaultConsumeThreadMax = 64;
    private static final Integer defaultConsumeThreadMin = 20;

    @Setter
    @Getter
    private int consumeMessageBatchMaxSize;


    @Override
    public void destroy() {
        this.setStarted(false);
        if (Objects.nonNull(consumer)) {
            consumer.shutdown();
        }
        log.info("container destroyed, {}", this);
    }

    public synchronized void start() throws MQClientException {

        if (this.isStarted()) {
            throw new IllegalStateException("container already started. " + this);
        }

        initRocketMQPushConsumer();

        // parse message type
        this.messageType = MqMessageUtil.getMessageTypeByCloudMQListener(rocketMQListener);
        log.debug("msgType: {}", messageType.getName());

        consumer.start();
        this.setStarted(true);

        log.info("started container: {}", this);
    }

    public class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {

        @SuppressWarnings("unchecked")
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            log.debug("concurrently received msgs size:{}", msgs.size());
            for (MessageExt messageExt : msgs) {
                log.debug("concurrently received msg: {}", messageExt);
                try {
                    consumeInner(messageExt);
                } catch (Exception e) {
                    log.error("consume message failed. messageExt:{}", messageExt, e);
                    context.setDelayLevelWhenNextConsume(delayLevelWhenNextConsume);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    public class DefaultMessageListenerOrderly implements MessageListenerOrderly {

        @SuppressWarnings("unchecked")
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            log.debug("orderly received msgs size:{}", msgs.size());
            for (MessageExt messageExt : msgs) {
                log.debug("orderly received msg: {}", messageExt);
                try {
                    consumeInner(messageExt);
                } catch (Exception e) {
                    log.error("consume message failed. messageExt:{}", messageExt, e);
                    context.setSuspendCurrentQueueTimeMillis(suspendCurrentQueueTimeMillis);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }
            return ConsumeOrderlyStatus.SUCCESS;
        }
    }

    private void consumeInner(MessageExt messageExt) throws Exception {
        long now = System.currentTimeMillis();
        ConsumerTimingSampleContext metricsContext = MetricsUtil.startConsumer(metricsProperty, metrics,
                messageExt.getTopic(), messageExt.getTags(), messageExt.getReconsumeTimes());

        if (TimeBasedJobProperties.JOB_TOPIC.equals(messageExt.getTopic())) {
            handleTimeBasedJob(messageExt, metricsContext);
        } else {
            try {
                rocketMQListener.onMessage(MqMessageUtil.doConvertMessageExtByClass(messageExt, messageType, true));
                //记录消费者情况
                MetricsUtil.recordConsumer(metricsProperty, metrics, metricsContext, null);
            } catch (Exception e) {
                //记录消费者情况
                MetricsUtil.recordConsumer(metricsProperty, metrics, metricsContext, e);
                //if (messageExt.getReconsumeTimes() >= consumer)
                throw e;
            }
        }

        long costTime = System.currentTimeMillis() - now;
        log.debug("consume {} cost: {} ms", messageExt.getMsgId(), costTime);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public String toString() {
        return "DefaultRocketMQListenerContainer{" +
                "consumerGroup='" + consumerGroup + '\'' +
                ", nameServer='" + nameServer + '\'' +
                ", subscription='" + subscription + '\'' +
                ", consumeMode=" + consumeMode +
                ", selectorType=" + selectorType +
                ", messageModel=" + messageModel +
                ", consumeTreadMax=" + consumeThreadMax +
                ", consumeTreadMin=" + consumeThreadMin +
                '}';
    }


    /**
     * 处理定时任务
     */
    private void handleTimeBasedJob(MessageExt messageExt, ConsumerTimingSampleContext metricsContext) {
        String str = new String(messageExt.getBody(), Charset.forName(charset));
        TimeBasedJobMessage timeBasedJobMessage = null;
        try {
            timeBasedJobMessage = JsonUtil.toBean(str, TimeBasedJobMessage.class);
            TimeBasedJobMessage finalTimeBasedJob = timeBasedJobMessage;
            timeBaseJobExecutor.execute(() -> {
                try {
                    long now = System.currentTimeMillis();
                    //发送历史久远的定时任务 丢弃
                    if (now - messageExt.getBornTimestamp() > discardTaskSeconds * 1000) {
                        long discardedCount = discardedTaskCount.incrementAndGet();
                        if (discardedCount % 100 == 0) {
                            log.warn("execute job discard history job {} timestamp {}", messageExt.getTags(), now);
                        }
                        throw new DiscardOldJobException(messageExt.getBornTimestamp());
                    }

                    log.info("received job {}: {}", messageExt.getTags(), str);

                    rocketMQListener.onMessage(MqMessageUtil.doConvertMessageExtByClass(messageExt, messageType, false));

                    try {
                        TimeBasedJobFeedback successFeedback = new TimeBasedJobFeedback();
                        successFeedback.setLogId(finalTimeBasedJob.getLogId());
                        successFeedback.setSuccess(true);
                        successFeedback.setMsg(null);
                        rocketMQTemplate.send(packageFeedbackPushMessage(finalTimeBasedJob, successFeedback));

                        log.info("finish job {}: {}", messageExt.getTags(), str);
                    } catch (Exception e) {
                        log.error("send job execute feedback error", e);
                    }
                    //记录消费者情况
                    MetricsUtil.recordConsumer(metricsProperty, metrics, metricsContext, null);
                } catch (Throwable throwable) {
                    handleTimeBasedJobException(str, finalTimeBasedJob, throwable, metricsContext);
                }
            });
        } catch (Exception e) {
            handleTimeBasedJobException(str, timeBasedJobMessage, e, metricsContext);
        }
    }


    private void handleTimeBasedJobException(String str, TimeBasedJobMessage timeBasedJobMessage, Throwable throwable, ConsumerTimingSampleContext metricsContext) {
        //记录消费者情况
        MetricsUtil.recordConsumer(metricsProperty, metrics, metricsContext, throwable);

        if (!(throwable instanceof DiscardOldJobException)) {
            log.error("execute job error " + str, throwable);
        }
        if (timeBasedJobMessage != null) {
            try {
                TimeBasedJobFeedback failFeedback = new TimeBasedJobFeedback();
                failFeedback.setLogId(timeBasedJobMessage.getLogId());
                failFeedback.setSuccess(false);
                failFeedback.setMsg(throwable.getClass().getName() + ", cause: " + throwable.getMessage());
                rocketMQTemplate.send(packageFeedbackPushMessage(timeBasedJobMessage, failFeedback));
            } catch (Exception e1) {
                log.error("send job execute feedback error", e1);
            }
        }
    }


    private PushMessage packageFeedbackPushMessage(TimeBasedJobMessage message, TimeBasedJobFeedback feedback) {
        PushMessage pushMessage = new PushMessage();
        pushMessage.setTopic(TimeBasedJobProperties.JOB_TOPIC_FEEDBACK);
        pushMessage.setEventCode(TimeBasedJobProperties.JOB_EVENTCODE_FEEDBACK);
        pushMessage.setKey(String.valueOf(message.getLogId()));
        pushMessage.setPayload(feedback);
        return pushMessage;
    }


    private void initRocketMQPushConsumer() throws MQClientException {

        Assert.notNull(rocketMQListener, "Property 'rocketMQListener' is required");
        Assert.notNull(consumerGroup, "Property 'consumerGroup' is required");
        Assert.notNull(nameServer, "Property 'nameServer' is required");
        Assert.notNull(subscription, "Property 'subscription' is required");
        //分配策略 改为环形分配（使得队列消费范围较为均匀）
        AllocateMessageQueueStrategy allocateMessageQueueStrategy = new AllocateMessageQueueAveragelyByCircle();

        consumer = new DefaultMQPushConsumer(consumerGroup, rpcHook, allocateMessageQueueStrategy, enableMsgTrace, null);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeThreadMax(consumeThreadMax);
        consumeThreadMin = Math.min(consumeThreadMin, consumeThreadMax);
        consumer.setConsumeThreadMin(consumeThreadMin);
        consumer.setMessageModel(messageModel);
        consumer.setInstanceName(MqMessageUtil.getInstanceName(nameServer));
        //设置最大批量消费数量
        consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);

        switch (selectorType) {
            case TAG:
                for (Map.Entry<String, String> topicTags : subscription.entrySet()) {
                    consumer.subscribe(topicTags.getKey(), topicTags.getValue());
                }
                break;
            case SQL92:
                for (Map.Entry<String, String> topicTags : subscription.entrySet()) {
                    consumer.subscribe(topicTags.getKey(), MessageSelector.bySql(topicTags.getValue()));
                }
                break;
            default:
                throw new IllegalArgumentException("Property 'selectorType' was wrong.");
        }

        //从上下文可以保证，每一个[top evenCode]消息，都有一个[top evenCode] listener 处理相应的逻辑

        switch (consumeMode) {
            case ORDERLY:
                consumer.setMessageListener(new DefaultMessageListenerOrderly());
                break;
            case CONCURRENTLY:
                consumer.setMessageListener(new DefaultMessageListenerConcurrently());
                break;
            default:
                throw new IllegalArgumentException("Property 'consumeMode' was wrong.");
        }

        if (rocketMQListener instanceof RocketMQPushConsumerLifecycleListener) {
            ((RocketMQPushConsumerLifecycleListener) rocketMQListener).prepareStart(consumer);
        }
    }

}
