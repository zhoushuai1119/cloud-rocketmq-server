package com.cloud.mq.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class MQConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MQConsumerApplication.class, args);
    }

}

