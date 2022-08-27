package com.cloud.platform.rocketmq.core;


import com.cloud.mq.base.dto.CloudMessage;
import org.apache.rocketmq.client.producer.LocalTransactionState;

/**
 * 分topic 事务逻辑
 *
 * @author zhou shuai
 */
public interface TopicTransactionListener<T,R> {

    /**
     * 本地事务方法
     * @param message 推送的消息
     * @param arg 业务参数,发送消息时传入，在这里可以取出
     *
     * @return 事务消息状态 成功或者失败
     *  成功： TransactionStateEnum.COMMIT_MESSAGE 允许订阅方消费该消息。
     *  失败： TransactionStateEnum.ROLLBACK_MESSAGE  消息将被丢弃
     *
     */
    LocalTransactionState executeTransaction(CloudMessage<T> message, R arg);

    /**
     * 回查事务方法
     * @param message 消息体
     *
     * @return 事务消息状态 成功或者失败
     *  TransactionStateEnum.COMMIT_MESSAGE  允许订阅方消费该消息。
     *  TransactionStateEnum.ROLLBACK_MESSAGE   消息将被丢弃。
     *  TransactionStateEnum.UNKNOW | Exception：继续执行回查逻辑（最多15次后丢弃消息）
     */
    LocalTransactionState checkLocalTransaction(CloudMessage<T> message);

}
