package com.cloud.platform.rocketmq.timedjob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时任务消息
 *
 * @Author Wang Lin(王霖)
 * @Date 2018/2/26
 * @Time 上午10:38
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeBasedJobMessage {
    /**
     * logId
     */
    Long logId;

    /**
     * 时间戳
     */
    long timestamp;
}
