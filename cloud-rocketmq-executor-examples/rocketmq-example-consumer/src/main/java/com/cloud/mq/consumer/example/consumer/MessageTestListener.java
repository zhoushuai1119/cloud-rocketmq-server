package com.cloud.mq.consumer.example.consumer;


import com.cloud.mq.base.dto.CloudMessage;
import com.cloud.platform.rocketmq.annotation.ConsumeTopic;
import com.cloud.platform.rocketmq.core.TopicListener;
import lombok.extern.slf4j.Slf4j;


/**
 * @description: 监听普通消息
 * @author: zhou shuai
 * @date: 2022/1/12 16:26
 * @version: v1
 */
@Slf4j
@ConsumeTopic(topic = "TP_TEST_TOPIC", eventCode = "EC_TEST_CODE_V2", log = true)
public class MessageTestListener implements TopicListener<String> {

    @Override
    public void onMessage(CloudMessage<String> message) {
        log.info("EC_TEST_CODE_V2接收到消息:{}",message.getPayload());
    }

}
