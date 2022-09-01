package com.cloud.mq.consumer.example.consumer;


import com.cloud.mq.base.dto.CloudMessage;
import com.cloud.mq.consumer.example.dto.SendDTO;
import com.cloud.platform.common.utils.JsonUtil;
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
@ConsumeTopic(topic = "TP_DTO_TEST_TOPIC", eventCode = "EC_TEST_DTO", log = true)
public class MessageDTOTestListener implements TopicListener<SendDTO> {

    @Override
    public void onMessage(CloudMessage<SendDTO> message) {
        log.info("EC_TEST_DTO接收到消息:{}", JsonUtil.toString(message.getPayload()));
    }

}
