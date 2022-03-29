package com.cloud.mq.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class MQProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MQProducerApplication.class, args);
    }

}

