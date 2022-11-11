package com.cloud.platform.rocketmq.metrics;

import com.cloud.platform.rocketmq.RocketMQProperties;
import com.cloud.platform.rocketmq.core.RocketMQTemplate;
import com.cloud.platform.rocketmq.metrics.impl.ConsumerTimingSampleContextImpl;
import com.cloud.platform.rocketmq.metrics.impl.MQMetricsImpl;
import com.cloud.platform.rocketmq.metrics.impl.ProducerTimingSampleContextImpl;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description: RocketMQ Metrics 自动配置
 * @author: zhou shuai
 * @date: 2022/11/10 13:59
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@Order
@Slf4j
public class MQMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean({RocketMQTemplate.class})
    public MQMetrics getProducerMetrics(RocketMQTemplate rocketMQTemplate, MeterRegistry registry) {
        MQMetrics metrics = new MQMetricsImpl(registry);
        rocketMQTemplate.setMetrics(metrics);
        return metrics;
    }

    @Bean
    @Order(0)
    @ConditionalOnBean({RocketMQTemplate.class})
    public MeterRegistryCustomizer limitCardinalityMQProducer(RocketMQProperties rocketMQProperties) {
        return getLimitFilter(ProducerTimingSampleContextImpl.PRODUCER_METRICS_NAME, rocketMQProperties.getMetrics().getMaxLabelCount());
    }

    @Bean
    @ConditionalOnMissingBean(MQMetrics.class)
    public MQMetrics getConsumerMetrics(MeterRegistry registry) {
        return new MQMetricsImpl(registry);
    }

    @Bean
    @Order(0)
    public MeterRegistryCustomizer limitCardinalityMQConsumer(RocketMQProperties rocketMQProperties) {
        return getLimitFilter(ConsumerTimingSampleContextImpl.CONSUME_METRICS_NAME, rocketMQProperties.getMetrics().getMaxLabelCount());
    }

    private MeterRegistryCustomizer getLimitFilter(String metricName, int limit) {
        return r -> r.config().meterFilter(MeterFilter.maximumAllowableTags(metricName,
                "key", limit, new MeterFilter() {
                    private AtomicBoolean alreadyWarned = new AtomicBoolean(false);

                    @Override
                    @NonNull
                    public MeterFilterReply accept(@NonNull Meter.Id id) {
                        if (alreadyWarned.compareAndSet(false, true)) {
                            log.warn("Reached the maximum number of MQ tags for '" + metricName + "'.");
                        }
                        return MeterFilterReply.DENY;
                    }
                })
        );
    }
}
