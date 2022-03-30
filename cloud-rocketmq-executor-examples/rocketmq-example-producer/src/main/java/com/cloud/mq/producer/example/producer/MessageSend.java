package com.cloud.mq.producer.example.producer;

import com.cloud.mq.base.core.CloudMQTemplate;
import com.cloud.mq.producer.example.producer.transaction.TransactionExecutor;
import com.cloud.platform.common.response.BaseResponse;
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


    public BaseResponse sendMessage() {
        final String message = "hello consumer i am producer!!!";
        BaseResponse result =  cloudMQTemplate.send("TP_TEST_TOPIC", "EC_TEST_CODE", message);
        return result;
    }

    public void sendTransactionMessage() {
        final String transactionMessage = "hello consumer i am transaction message!!!";
        transactionExecutor.send(transactionMessage, "messageId");
    }


}
