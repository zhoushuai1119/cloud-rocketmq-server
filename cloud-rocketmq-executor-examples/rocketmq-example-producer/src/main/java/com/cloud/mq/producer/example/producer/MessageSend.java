package com.cloud.mq.producer.example.producer;

import com.cloud.mq.base.core.CloudMQTemplate;
import com.cloud.mq.producer.example.producer.transaction.TransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: zhou shuai
 * @date: 2022/1/12 17:08
 * @version: v1
 */
@Slf4j
@Component
public class MessageSend {

    @Autowired
    private CloudMQTemplate cloudMQTemplate;

    @Autowired
    private TransactionExecutor transactionExecutor;


    public void sendMessage() {
        final String message = "hello consumer i am producer!!!";
        cloudMQTemplate.send("TP_TEST_TOPIC", "EC_TEST_CODE", message);
    }

    public void sendTransactionMessage() {
        final String transactionMessage = "hello consumer i am transaction message!!!";
        transactionExecutor.send(transactionMessage, "messageId");
    }


}
