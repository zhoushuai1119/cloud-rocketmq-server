package com.cloud.platform.rocketmq;

import com.beust.jcommander.internal.Lists;
import com.cloud.mq.base.core.CloudMQListener;
import com.cloud.platform.rocketmq.annotation.ConsumeTopic;
import com.cloud.platform.rocketmq.core.*;
import com.cloud.platform.rocketmq.enums.ConsumeMode;
import com.cloud.platform.rocketmq.enums.SelectorType;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ自动配置
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

    @Bean
    @ConditionalOnClass(DefaultMQProducer.class)
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    @ConditionalOnProperty(prefix = "cloud.rocketmq", value = {"name-server", "producer.group"})
    public DefaultMQProducer mqProducer(RocketMQProperties rocketMQProperties) {

        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String groupName = producerConfig.getGroup();
        Assert.hasText(groupName, "[cloud.rocketmq.producer.group] must not be null");

        DefaultMQProducer producer = new DefaultMQProducer(producerConfig.getGroup(), producerConfig.isEnableMsgTrace());
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
     * mq 事务消息生产者
     *
     * @param rocketMQProperties
     * @return
     * @throws Exception
     */
    @Bean
    @ConditionalOnClass(value = {TransactionMQProducer.class})
    @ConditionalOnMissingBean(value = {TransactionMQProducer.class})
    public TransactionMQProducer mqTransactionProducer(RocketMQProperties rocketMQProperties) {
        //事务消息model
        RocketMQProperties.TransactionProducerCustom tranProCustomModel = rocketMQProperties.getTransactionProducerCustom();
        //普通消息model
        RocketMQProperties.Producer producerConfigModel = rocketMQProperties.getProducer();
        //生成事务消息生产groupName
        if (Objects.isNull(tranProCustomModel)) {
            //如果事务消息没有配置属性，则使用普通消息的属性组装生成model
            tranProCustomModel = new RocketMQProperties.TransactionProducerCustom();
            BeanUtils.copyProperties(producerConfigModel, tranProCustomModel);
        }
        String groupName = tranProCustomModel.getGroup() + "-transaction";
        Assert.hasText(groupName, "[cloud.rocketmq.producer.group] must not be null");

        //设置属性--事务消息生产
        //TransactionMQProducer producer = new TransactionMQProducer(groupName);
        TransactionMQProducer producer = new TransactionMQProducer(null, groupName, null, tranProCustomModel.isEnableMsgTrace(), null);
        producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        producer.setSendMsgTimeout(tranProCustomModel.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(tranProCustomModel.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(tranProCustomModel.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(tranProCustomModel.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(tranProCustomModel.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(tranProCustomModel.isRetryAnotherBrokerWhenNotStoreOk());

        // 设置属性--回查事务线程池
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
        //设置属性--执行事务监听器
        producer.setTransactionListener(new DefaultTopicTransactionListenerImpl());

        return producer;
    }

    /**
     * mq 事务消息模板类
     *
     * @param mqProducer
     * @return
     */
    @Bean(destroyMethod = "destroy")//销毁的时候调用对象的 destroy 方法
    @ConditionalOnBean(value = {TransactionMQProducer.class})
    @ConditionalOnMissingBean(name = "rocketMQTransactionTemplate")
    public RocketMQTransactionTemplate rocketMQTransactionTemplate(TransactionMQProducer mqProducer) {
        RocketMQTransactionTemplate rocketMQTransactionTemplate = new RocketMQTransactionTemplate();
        rocketMQTransactionTemplate.setProducer(mqProducer);
        return rocketMQTransactionTemplate;
    }


    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(name = "rocketMQTemplate")
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer mqProducer) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(mqProducer);
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
        private RocketMQProperties rocketMQProperties;

        @Autowired(required = false)
        private RocketMQTemplate rocketMQTemplate;

        @Value("${eureka.instance.instanceId:unknown}")
        private String instanceId;

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
            if (MapUtils.isNotEmpty(rocketMQProperties.getConsumer())) {
                //获取带有 @Component 的实现listener
                Map<String, CloudMQListener> beans = this.applicationContext.getBeansOfType(CloudMQListener.class);

                //如果MonsterMQListener是空的，注册空的MonsterMQListener，找TopicListener bean，分topic消费消息
                if (MapUtils.isEmpty(beans)) {
                    Map<String/*beanName*/, TopicListener> beanMap = this.applicationContext.getBeansOfType(TopicListener.class);
                    if (MapUtils.isEmpty(beanMap)) {
                        throw new Exception("fail to find rocketMQ message listener");
                    }

                    //设置 topicAndEventCode  和 listener 关系
                    Map<String/*topic:eventCode*/, DefaultMessageListener.ConsumeTopicInfo> topicMap = new HashMap<>();
                    Map<String, List<String>> topicAndEventCodes = new HashMap<>();
                    Iterator<TopicListener> topicListenerIterator = beanMap.values().iterator();
                    while (topicListenerIterator.hasNext()) {
                        TopicListener topicListener = topicListenerIterator.next();
                        //listener 注解中的 topicAndEventCode 得到 topicInfo
                        DefaultMessageListener.ConsumeTopicInfo topicInfo = getConsumeTopicAndEventCode(topicListener);
                        if (Objects.nonNull(topicInfo)) {
                            String topicAndEventCode = topicInfo.getTopic() + ":" + topicInfo.getEventCode();

                            DefaultMessageListener.ConsumeTopicInfo existed = topicMap.get(topicAndEventCode);
                            if (Objects.nonNull(existed)) {
                                throw new Exception("duplicated topic eventCode listener: " + existed.getTopicListener().getClass().getName() + " and " + topicListener.getClass().getName());
                            } else {
                                topicInfo.setTopicListener(topicListener);
                                topicMap.put(topicAndEventCode, topicInfo);

                                //设置listener 中 topic 和eventCode 关系
                                List<String> eventCodes = topicAndEventCodes.get(topicInfo.getTopic());
                                if (CollectionUtils.isEmpty(eventCodes)) {
                                    eventCodes = Lists.newArrayList();
                                }
                                eventCodes.add(topicInfo.getEventCode());
                                topicAndEventCodes.put(topicInfo.getTopic(), eventCodes);
                            }
                        }
                    }

                    //校验topic eventCode 是否正确配置
                    if (rocketMQProperties.getTopicRelationCheck()) {
                        //代码文件里的topic eventCode
                        for (Map.Entry<String, List<String>> entry : topicAndEventCodes.entrySet()) {
                            List<String> eventCodes = entry.getValue();
                            if (eventCodes.size() > 1 && eventCodes.contains("*")) {
                                throw new Exception("fail in listener code ! configured eventCode * ,cannot exist at the same time many EventCode " + eventCodes.toString());
                            }
                        }

                        //配置文件里的topic eventCode
                        Set<String> topicEventCodesInConfigs = new HashSet<>();
                        for (RocketMQProperties.Consumer consumer : rocketMQProperties.getConsumer().values()) {
                            Map<String, String> subscription = consumer.getSubscription();
                            if (MapUtils.isNotEmpty(subscription)) {
                                for (Map.Entry<String, String> topicEntry : subscription.entrySet()) {
                                    if (StringUtils.isNotBlank(topicEntry.getValue())) {
                                        String topic = topicEntry.getKey();
                                        List<String> eventCodeList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(topicEntry.getValue());
                                        if (CollectionUtils.isNotEmpty(eventCodeList)) {
                                            if (eventCodeList.size() > 1 && eventCodeList.contains("*")) {
                                                throw new Exception("fail in config ! configured eventCode * ,cannot exist at the same time many EventCode " + eventCodeList);
                                            }
                                            eventCodeList.forEach(eventCode -> {
                                                topicEventCodesInConfigs.add(topic + ":" + eventCode);
                                            });
                                        }
                                    }
                                }
                            }
                        }

                        for (String topicEventCodesInConfig : topicEventCodesInConfigs) {
                            //校验配置文件里的topic是否都有listener
                            if (!topicMap.containsKey(topicEventCodesInConfig)) {
                                throw new Exception("fail to find topic listener for " + topicEventCodesInConfig);
                            }
                        }
                        for (String topicEventCodesInCode : topicMap.keySet()) {
                            //校验代码里的topic是否在配置文件里
                            if (!topicEventCodesInConfigs.contains(topicEventCodesInCode)) {
                                throw new Exception("fail to find topic eventCode config for " + topicEventCodesInCode);
                            }
                        }
                    }

                    //为消费者,设置对应的监听Listener （包括topic 和 evencode关系）
                    for (Map.Entry<String, RocketMQProperties.Consumer> consumer : rocketMQProperties.getConsumer().entrySet()) {
                        DefaultMessageListener messageListener = new DefaultMessageListener(topicMap);
                        ///注册单例bean
                        applicationContext.getBeanFactory().registerSingleton(consumer.getKey(), messageListener);
                    }
                    //执行完成后，bean 就有MonsterMQListener 对象了（DefaultMessageListener --extends --> 就有MonsterMQListener）
                    beans = this.applicationContext.getBeansOfType(CloudMQListener.class);
                }

                if (MapUtils.isNotEmpty(beans)) {
                    //根据上下文得到 beanName rocketMQListener 等信息  实例化启动，消费者
                    beans.forEach((beanName, rocketMQListener) -> registerContainer(beanName, rocketMQListener, rocketMQTemplate, instanceId));
                } else {
                    throw new Exception("fail to find rocketMQ message listener");
                }
            }
        }

        /**
         * 从ConsumeTopic注解中获取topic和eventCode
         *
         * @param consumeTopic
         * @return
         */
        private DefaultMessageListener.ConsumeTopicInfo getConsumeTopicAndEventCode(TopicListener consumeTopic) {
            Annotation annotation = null;
            Class clazz = consumeTopic.getClass();
            if (AopUtils.isAopProxy(consumeTopic)) {
                //@Transactional会增加AOP代理，查找真实的类对象
                clazz = ((Advised) consumeTopic).getTargetSource().getTargetClass();
            }
            annotation = clazz.getAnnotation(ConsumeTopic.class);

            if (Objects.isNull(annotation)) {
                log.error("topic listener {} has no cnsumetopic annotation", consumeTopic.getClass().getName());
            } else {
                DefaultMessageListener.ConsumeTopicInfo topicInfo = null;
                Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(annotation);
                if (MapUtils.isNotEmpty(attrs) && attrs.containsKey("topic") && attrs.containsKey("eventCode")) {
                    topicInfo = new DefaultMessageListener.ConsumeTopicInfo();
                    topicInfo.setTopic(attrs.get("topic").toString());
                    topicInfo.setEventCode(attrs.get("eventCode").toString());
                    if (attrs.containsKey("log")) {
                        topicInfo.setLog(Boolean.valueOf(attrs.get("log").toString()));
                    }
                }
                return topicInfo;
            }
            return null;
        }

        private void registerContainer(String beanName, CloudMQListener rocketMQListener, RocketMQTemplate rocketMQTemplate, String instanceId) {

            Assert.notNull(rocketMQProperties.getConsumer(), "[cloud.rocketmq.consumer] must not be null");
            RocketMQProperties.Consumer consumerProperties = rocketMQProperties.getConsumer().get(beanName);
            Assert.notNull(consumerProperties, "[cloud.rocketmq.consumer." + beanName + "] must not be null");
            ConsumeMode consumeMode = consumerProperties.isOrderly() ? ConsumeMode.ORDERLY : ConsumeMode.CONCURRENTLY;
            int customConsumeMessageBatchMaxSize = Math.max(1, consumerProperties.getConsumeMessageBatchMaxSize());
            MessageModel messageModel = consumerProperties.isBroadcasting() ? MessageModel.BROADCASTING : MessageModel.CLUSTERING;
            SelectorType selectorType = getSelectorType(consumerProperties);
            //普通topic key: topic, value: tags 格式{ec_0||ec_1}
            Map<String, String> topicTagsMap = new HashMap<>();
            //包含定时任务
            boolean isContainsTimedTask = false;
            for (Map.Entry<String, String> topicEventCodes : consumerProperties.getSubscription().entrySet()) {
                Assert.hasText(topicEventCodes.getValue(), "[cloud.rocketmq.consumer." + beanName + "." + topicEventCodes.getKey() + "] must not be null");
                if ("*".equals(topicEventCodes.getValue().trim())) {
                    //支持在同一topic下，不同event_code支持通配符（如*）的方式
                    topicTagsMap.put(topicEventCodes.getKey(), "*");
                } else {
                    String tags = topicEventCodes.getValue().replace(",", "||");
                    topicTagsMap.put(topicEventCodes.getKey(), tags);
                }
                if (TimeBasedJobProperties.JOB_TOPIC.equals(topicEventCodes.getKey())) {
                    isContainsTimedTask = true;
                }
            }

            ExecutorService timedJobExecutor = null;
            if (isContainsTimedTask) {
                Assert.isTrue(timeBasedJobProperties != null && timeBasedJobProperties.isEnabled(), "[cloud.time-based-job.enabled] must be true");
                Assert.notNull(rocketMQProperties.getProducer(), "[cloud.rocketmq.producer] must not be null");
                Assert.hasText(rocketMQProperties.getProducer().getGroup(), "[cloud.rocketmq.producer.group] must not be null");
                //定时任务异步处理
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
            //beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_INSTANCE_ID, instanceId);
            beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.PROP_CONSUME_MESSAGE_BATCH_MAX_SIZE, customConsumeMessageBatchMaxSize);
            //beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.THREAD_POOL_METER_REGISTRY, meterRegistry);
            //beanBuilder.addPropertyValue(DefaultRocketMQListenerContainerConstants.RPC_HOOK, aclRPCHook);
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
            if ("SQL92".equals(consumerProperties.getSelectorType())) {
                return SelectorType.SQL92;
            }
            if ("TAG".equals(consumerProperties.getSelectorType())) {
                return SelectorType.TAG;
            }
            log.warn("select miss！miss value is {} ,use default SelectorType TAG，", consumerProperties.getSelectorType());
            return SelectorType.TAG;
        }
    }


}
