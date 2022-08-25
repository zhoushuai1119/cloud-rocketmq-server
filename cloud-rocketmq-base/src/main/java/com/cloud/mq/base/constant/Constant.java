package com.cloud.mq.base.constant;

/**
 * @description:
 * @author: zhou shuai
 * @date: 2022/8/25 13:47
 * @version: v1
 */
public class Constant {

    public interface TopicInfo {
        /**
         * topic
         */
        String TOPIC = "topic";

        /**
         * eventCode
         */
        String EVENT_CODE = "eventCode";

        /**
         * log
         */
        String LOG = "log";
    }

    public interface SelectorType {
        /**
         * SQL92
         */
        String SQL92 = "SQL92";

        /**
         * TAG
         */
        String TAG = "TAG";
    }

}
