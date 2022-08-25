package com.cloud.mq.producer;

import com.cloud.mq.producer.example.producer.MessageSend;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest()
@RunWith(SpringRunner.class)
@Slf4j
public class MQProducerApplicationTest {

    @Autowired
    private MessageSend messageSend;


    /**
     * 发送普通消息测试
     */
    @Test
    public void messageSendTest(){
        messageSend.sendMessage();
    }

    /**
     * 发送事务消息测试
     */
    @Test
    public void sendTransactionMessageTest(){
        messageSend.sendTransactionMessage();
    }


}

