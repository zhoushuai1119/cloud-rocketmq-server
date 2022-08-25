package com.cloud.platform.rocketmq.timedjob;

import lombok.Data;

/**
 * 定时任务反馈
 *
 * @author shuai.zhou
 * @date 2018/2/26
 * @time 上午10:38
 */
@Data
public class TimeBasedJobFeedback {
    /**
     * logId
     */
    Long logId;

    /**
     * 是否成功
     */
    Boolean success;

    /**
     * 备注
     */
    String msg;
}
