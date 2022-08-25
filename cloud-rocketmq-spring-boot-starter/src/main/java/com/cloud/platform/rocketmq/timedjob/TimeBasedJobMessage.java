package com.cloud.platform.rocketmq.timedjob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时任务消息
 *
 * @author shuai.zhou
 * @date 2018/2/26
 * @time 上午10:38
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
