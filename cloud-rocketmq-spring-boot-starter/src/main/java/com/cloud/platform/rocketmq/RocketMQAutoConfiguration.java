package com.cloud.platform.rocketmq;

import com.cloud.mq.base.constant.Constant;
import com.cloud.mq.base.core.CloudMQListener;
import com.cloud.platform.common.constants.PlatformCommonConstant;
import com.cloud.platform.rocketmq.annotation.ConsumeTopic;
import com.cloud.platform.rocketmq.core.*;
import com.cloud.platform.rocketmq.enums.ConsumeMode;
import com.cloud.platform.rocketmq.enums.SelectorType;
import com.cloud.platform.rocketmq.metrics.MQMetrics;
import com.cloud.platform.rocketmq.metrics.impl.MQMetricsImpl;
import com.google.common.base.Joiner;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ????????????
 *
 * @author shuai.zhou
 */
@Configuration
@EnableConfigurationProperties({RocketMQProperties.class, TimeBasedJobProperties.class})
@ConditionalOnClass(MQClientAPIImpl.class)
@Order
@Slf4j
public class RocketMQAutoConfiguration {

    static {
        System.setProperty("rocketmq.client.log.loadconfig", "false");
        System.setProperty("rocketmq.client.logUseSlf4j", "true");
    }

    /**
     * ???????????? acl??????
     */
    @Value("${cloud.rocketmq.acl.accessKey:-}")
    private String accesskey;
    /**
     * ???????????? acl??????
     */
    @Value("${cloud.rocketmq.acl.secretKey:-}")
    private String secretkey;

    @Bean
    public AclClientRPCHook aclRPCHook() {
        return new AclClientRPCHook(new SessionCredentials(accesskey, secretkey));
    }

    @Bean
    @ConditionalOnClass(DefaultMQProducer.class)
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    @ConditionalOnProperty(prefix = "cloud.rocketmq", value = {"name-server", "producer.group-name"})
    public DefaultMQProducer mqProducer(RocketMQProperties rocketMQProperties, AclClientRPCHook aclRPCHook) {

        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        String groupName = producerConfig.getGroupName();
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[rocketmq.producer.group] must not be null");

        boolean isEnableMsgTrace = producerConfig.isEnableMsgTrace();
        String customizedTraceTopic = producerConfig.getCustomizedTraceTopic();

        DefaultMQProducer producer = new DefaultMQProducer(producerConfig.getGroupName(), aclRPCHook, producerConfig.isEnableMsgTrace(), customizedTraceTopic);
        producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        producer.setSendMsgTimeout(producerConfig.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryAnotherBrokerWhenNotStoreOk());
        return producer;
    }

    /**
     * mq ?????????????????????
     *
     * @param rocketMQProperties
     * @return
     * @throws Exception
     */
    @Bean
    @ConditionalOnBean(DefaultMQProducer.class)
    public TransactionMQProducer mqTransactionProducer(RocketMQProperties rocketMQProperties, AclClientRPCHook aclRPCHook) {
        //????????????model
        RocketMQProperties.TransactionProducerCustom tranProCustomModel = rocketMQProperties.getTransactionProducerCustom();
        //????????????model
        RocketMQProperties.Producer producerConfigModel = rocketMQProperties.getProducer();
        if (Objects.isNull(tranProCustomModel)) {
            //?????????????????????????????????????????????????????????????????????????????????model
            tranProCustomModel = new RocketMQProperties.TransactionProducerCustom();
            BeanUtils.copyProperties(producerConfigModel, tranProCustomModel);
        }
        Assert.hasText(tranProCustomModel.getGroupName(), "[cloud.rocketmq.transactionProducerCustom.groupName] must not be null");
        //????????????????????????
        String transactionGroupName = tranProCustomModel.getGroupName() + "-transaction";

        boolean isEnableMsgTrace = producerConfigModel.isEnableMsgTrace();
        String customizedTraceTopic = producerConfigModel.getCustomizedTraceTopic();

        //????????????--??????????????????
        //TransactionMQProducer producer = new TransactionMQProducer(groupName);
        TransactionMQProducer producer = new TransactionMQProducer(null, transactionGroupName, aclRPCHook, isEnableMsgTrace, customizedTraceTopic);
        producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        producer.setSendMsgTimeout(tranProCustomModel.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(tranProCustomModel.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(tranProCustomModel.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(tranProCustomModel.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(tranProCustomModel.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(tranProCustomModel.isRetryAnotherBrokerWhenNotStoreOk());

        // ????????????--?????????????????????
        RocketMQProperties.TransactionExecutorConf tEConf = tranProCustomModel.getCheckThreadPool();
        if (Objects.isNull(tEConf)) {
            tEConf = new RocketMQProperties.TransactionExecutorConf();
        }

        ExecutorService executorService = new ThreadPoolExecutor(
                tEConf.getCorePoolSize(), tEConf.getMaximumPoolSize(), tEConf.getKeepAliveTime(),
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("transac-check");
                        return thread;
                    }
                });
        producer.setExecutorService(executorService);
        //????????????--?????????????????????
        producer.setTransactionListener(new DefaultTopicTransactionListenerImpl());

        return producer;
    }

    @Bean
    @ConditionalOnClass({MeterRegistry.class})
    public MQMetrics getProducerMetrics(MeterRegistry registry) {
        return new MQMetricsImpl(registry);
    }

    /**
     * mq ?????????????????????
     *
     * @param mqProducer
     * @return
     */
    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean({TransactionMQProducer.class, MQMetrics.class})
    @ConditionalOnMissingBean(name = "rocketMQTransactionTemplate")
    public RocketMQTransactionTemplate rocketMQTransactionTemplate(TransactionMQProducer mqProducer, MQMetrics mqMetrics, RocketMQProperties properties) {
        RocketMQTransactionTemplate rocketMQTransactionTemplate = new RocketMQTransactionTemplate();
        rocketMQTransactionTemplate.setProducer(mqProducer);
        rocketMQTransactionTemplate.setMetrics(mqMetrics);
        rocketMQTransactionTemplate.setMetricsProperty(properties.getMetrics());
        return rocketMQTransactionTemplate;
    }


    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean({DefaultMQProducer.class, MQMetrics.class})
    @ConditionalOnMissingBean(name = "rocketMQTemplate")
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer mqProducer, MQMetrics mqMetrics, RocketMQProperties properties) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(mqProducer);
        rocketMQTemplate.setMetrics(mqMetrics);
        rocketMQTemplate.setMetricsProperty(properties.getMetrics());
        return rocketMQTemplate;
    }

    @Configuration
    @ConditionalOnClass(DefaultMQPushConsumer.class)
    @EnableConfigurationProperties(RocketMQProperties.class)
    @ConditionalOnProperty(prefix = "cloud.rocketmq", value = "name-server")
    @Order
    public static class ListenerContainerConfiguration implements ApplicationContextAware, InitializingBean {
        private ConfigurableApplicationContext applicationContext;

        private AtomicLong counter = new AtomicLong(0);

        @Resource
        private AclClientRPCHook aclRPCHook;

        @Resource
        private RocketMQProperties rocketMQProperties;

        @Autowired(required = false)
        private RocketMQTemplate rocketMQTemplate;

        @Autowired(required = false)
        private TimeBasedJobProperties timeBasedJobProperties;


        public ListenerContainerConfiguration() {

        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            if (Objects.nonNull(rocketMQProperties.getConsumer())) {
                String consumerGroupName = rocketMQProperties.getConsumer().getGroupName();
                Assert.notNull(consumerGroupName, "[cloud.rocketmq.consumer.groupName] must not be null");
                //??????CloudMQListener????????????????????????CloudMQListener??????TopicListener bean??????topic????????????
                Map<String/*beanName*/, TopicListener> beanMap = this.applicationContext.getBeansOfType(TopicListener.class);
                if (MapUtils.isEmpty(beanMap)) {
                    log.warn("fail to find rocketMQ message listener");
                    return;
                }
                //?????? topicAndEventCode  ??? listener ??????
                Map<String/*topic:eventCode*/, DefaultMessageListener.ConsumeTopicInfo> topicMap = new HashMap<>();
                Map<String/*topic*/, List<String>> topicAndEventCodes = new HashMap<>();
                Iterator<TopicListener> topicListenerIterator = beanMap.values().iterator();
                while (topicListenerIterator.hasNext()) {
                    TopicListener topicListener = topicListenerIterator.next();
                    //listener ???????????? topicAndEventCode ?????? topicInfo
                    DefaultMessageListener.ConsumeTopicInfo topicInfo = getConsumeTopicAndEventCode(topicListener);
                    if (Objects.nonNull(topicInfo)) {
                        String topicAndEventCode = topicInfo.getTopic() + ":" + topicInfo.getEventCode();

                        DefaultMessageListener.ConsumeTopicInfo existed = topicMap.get(topicAndEventCode);
                        if (Objects.nonNull(existed)) {
                            throw new Exception("duplicated topic eventCode listener: " + existed.getTopicListener().getClass().getName() + " and " + topicListener.getClass().getName());
                        } else {
                            topicInfo.setTopicListener(topicListener);
                            topicMap.put(topicAndEventCode, topicInfo);

                            //??????listener ??? topic ???eventCode ??????
                            List<String> eventCodes = topicAndEventCodes.get(topicInfo.getTopic());
                            if (CollectionUtils.isEmpty(eventCodes)) {
                                eventCodes = new ArrayList<>();
                            }
                            eventCodes.add(topicInfo.getEventCode());
                            topicAndEventCodes.put(topicInfo.getTopic(), eventCodes);
                        }
                    }
                }

                //?????????????????????*??????????????????
                for (Map.Entry<String, List<String>> entry : topicAndEventCodes.entrySet()) {
                    List<String> eventCodes = entry.getValue();
                    if (eventCodes.size() > 1 && eventCodes.contains(PlatformCommonConstant.SymbolParam.ALL_SYMBOL)) {
                        throw new Exception("fail in listener code ! configured eventCode * ,cannot exist at the same time many EventCode " + eventCodes);
                    }
                }

                //????????????,?????????????????????Listener ?????????topic ??? evencode?????????
                DefaultMessageListener messageListener = new DefaultMessageListener(topicMap);
                ///????????????bean
                applicationContext.getBeanFactory().registerSingleton(consumerGroupName, messageListener);
                //??????????????????bean ??????CloudMQListener ?????????
                Map<String/*consumerGroupName*/, CloudMQListener> beans = this.applicationContext.getBeansOfType(CloudMQListener.class);

                //CloudMQListener??????????????????????????????DefaultMessageListener
                if (MapUtils.isNotEmpty(beans) && beans.size() > 1) {
                    throw new Exception("please must not implements CloudMQListener interface, must only exist one DefaultMessageListener !!!");
                }

                //????????????
                Map<String, MQMetrics> metricsMap = this.applicationContext.getBeansOfType(MQMetrics.class);
                MQMetrics mqMetrics;
                if (MapUtils.isNotEmpty(metricsMap)) {
                    mqMetrics = metricsMap.values().iterator().next();
                } else {
                    mqMetrics = null;
                }

                //????????????????????? beanName rocketMQListener ?????????  ???????????????????????????
                beans.forEach((beanName, rocketMQListener) -> registerContainer(beanName, rocketMQListener, rocketMQTemplate, topicAndEventCodes, mqMetrics));
            }
        }

        /**
         * ???ConsumeTopic???????????????topic???eventCode
         *
         * @param consumeTopic
         * @return
         */
        private DefaultMessageListener.ConsumeTopicInfo getConsumeTopicAndEventCode(TopicListener consumeTopic) {
            //?????????????????????????????????(????????????@Transactional?????????aop????????????)
            Class<?> clazz = AopProxyUtils.ultimateTargetClass(consumeTopic);
            ConsumeTopic annotation = clazz.getAnnotation(ConsumeTopic.class);
            if (Objects.isNull(annotation)) {
                log.error("topic listener {} has no cnsumetopic annotation", consumeTopic.getClass().getName());
                return null;
            }
            DefaultMessageListener.ConsumeTopicInfo topicInfo = new DefaultMessageListener.ConsumeTopicInfo();
            topicInfo.setTopic(annotation.topic());
            topicInfo.setEventCode(annotation.eventCode());
            topicInfo.setLog(annotation.log());
            return topicInfo;
        }

        private void registerContainer(String beanName, CloudMQListener rocketMQListener, RocketMQTemplate rocketMQTemplate, Map<String, List<String>> topicAndEventCodes, MQMetrics mqMetrics) {

            Assert.notNull(rocketMQProperties.getConsumer(), "[cloud.rocketmq.consumer] must not be null");
            RocketMQProperties.Consumer consumerProperties = rocketMQProperties.getConsumer();
            Assert.notNull(consumerProperties.getGroupName(), "[cloud.rocketmq.consumer.groupName] must not be null");
            ConsumeMode consumeMode = consumerProperties.isOrderly() ? ConsumeMode.ORDERLY : ConsumeMode.CONCURRENTLY;
            int customConsumeMessageBatchMaxSize = Math.max(1, consumerProperties.getConsumeMessageBatchMaxSize());
            MessageModel messageModel = consumerProperties.isBroadcasting() ? MessageModel.BROADCASTING : MessageModel.CLUSTERING;
            SelectorType selectorType = getSelectorType(consumerProperties);
            //??????topic key: topic, value: tags ??????{ec_0||ec_1}
            Map<String, String> topicTagsMap = new HashMap<>();
            //????????????????????????
            AtomicBoolean isContainsTimedTask = new AtomicBoolean(false);
            topicAndEventCodes.forEach((topic, eventCodeList) -> {
                String eventCodeListStr = Joiner.on(PlatformCommonConstant.SymbolParam.OR_SYMBOL).skipNulls().join(eventCodeList);
                if (Objects.equals(PlatformCommonConstant.SymbolParam.ALL_SYMBOL, eventCodeListStr.trim())) {
                    //???????????????topic????????????event_code?????????????????????*????????????
                    topicTagsMap.put(topic, PlatformCommonConstant.SymbolParam.ALL_SYMBOL);
                } else {
                    topicTagsMap.put(topic, eventCodeListStr);
                }
                if (Objects.equals(TimeBasedJobProperties.JOB_TOPIC, topic)) {
                    isContainsTimedTask.set(true);
                }
            });

            ExecutorService timedJobExecutor = null;
            if (isContainsTimedTask.get()) {
                Assert.isTrue(timeBasedJobProperties != null && timeBasedJobProperties.isEnabled(), "[cloud.time-based-job.enabled] must be true");
                Assert.notNull(rocketMQProperties.getProducer(), "[cloud.rocketmq.producer] must not be null");
                Assert.hasText(rocketMQProperties.getProducer().getGroupName(), "[cloud.rocketmq.producer.groupName] must not be null");
                //????????????????????????
                timedJobExecutor = new ThreadPoolExecutor(timeBasedJobProperties.getThreadPoolSize(), timeBasedJobProperties.getThreadPoolSize(),
                        0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new MQThreadFactory("timed-job"));
            }


            BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultRocketMQListenerContainer.class);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_NAMESERVER, rocketMQProperties.getNameServer());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_TOPIC_MAP, topicTagsMap);

            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUMER_GROUP, beanName);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_ENABLE_MSG_TRACE, consumerProperties.isEnableMsgTrace());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUME_MODE, consumeMode);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUME_THREAD_MAX, consumerProperties.getConsumeThreadMax());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUME_THREAD_MIN, consumerProperties.getConsumeThreadMin());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_MESSAGE_MODEL, messageModel);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_SELECTOR_TYPE, selectorType);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_ROCKETMQ_LISTENER, rocketMQListener);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_TIME_BASED_JOB_EXECUTOR, timedJobExecutor);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROD_DISCARD_TASK_SECONDS, timeBasedJobProperties.getDiscardTaskSeconds());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_ROCKETMQ_TEMPLATE, rocketMQTemplate);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_METRICS, mqMetrics);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_METRICS_PROPERTY, rocketMQProperties.getMetrics() == null ? new RocketMQProperties.Metrics() : rocketMQProperties.getMetrics());
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUME_MESSAGE_BATCH_MAX_SIZE, customConsumeMessageBatchMaxSize);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.RPC_HOOK, aclRPCHook);
            beanBuilder.setDestroyMethodName(DefaultRocketMQListenerContainerConstants.METHOD_DESTROY);

            String containerBeanName = String.format("%s_%s", DefaultRocketMQListenerContainer.class.getName(), counter.incrementAndGet());
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());

            DefaultRocketMQListenerContainer container = beanFactory.getBean(containerBeanName, DefaultRocketMQListenerContainer.class);

            if (!container.isStarted()) {
                try {
                    container.start();
                } catch (Exception e) {
                    log.error("started container failed. {}", container, e);
                    throw new RuntimeException(e);
                }
            }

            log.info("register rocketMQ listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
        }

        private SelectorType getSelectorType(RocketMQProperties.Consumer consumerProperties) {
            if (Constant.SelectorType.SQL92.equals(consumerProperties.getSelectorType())) {
                return SelectorType.SQL92;
            }
            if (Constant.SelectorType.TAG.equals(consumerProperties.getSelectorType())) {
                return SelectorType.TAG;
            }
            log.warn("select miss???miss value is {} ,use default SelectorType TAG???", consumerProperties.getSelectorType());
            return SelectorType.TAG;
        }
    }


}
