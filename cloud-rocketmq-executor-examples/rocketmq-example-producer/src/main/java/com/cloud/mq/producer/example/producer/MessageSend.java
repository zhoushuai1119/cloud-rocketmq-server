package com.cloud.mq.producer.example.producer;

import com.cloud.mq.base.core.CloudMQTemplate;
import com.cloud.mq.producer.example.producer.transaction.TransactionExecutor;
import com.cloud.platform.common.domain.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 发送普通消息
     */
    public BaseResponse sendMessage() {
        for (int i = 0; i < 20; i++) {
            final String message = "hello consumer i am producer!!!" + i;
            cloudMQTemplate.send("TP_TEST_TOPIC", "EC_TEST_CODE", message);
            cloudMQTemplate.send("TP_TEST_TOPIC", "EC_TEST_CODE_V2", message);
        }
        return BaseResponse.createSuccessResult(null);
    }

    /**
     * 发送事务消息
     */
    public void sendTransactionMessage() {
        final String transactionMessage = "hello consumer i am transaction message!!!";
        transactionExecutor.send(transactionMessage, "messageId");
    }


}
