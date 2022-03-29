package com.cloud.platform.rocketmq.core;

import com.cloud.mq.base.core.CloudMQListener;
import org.springframework.beans.factory.DisposableBean;

public interface RocketMQListenerContainer extends DisposableBean {

    void setupMessageListener(CloudMQListener<?> messageListener);
}
