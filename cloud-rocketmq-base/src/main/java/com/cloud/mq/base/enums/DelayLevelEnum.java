package com.cloud.mq.base.enums;

import lombok.Getter;

/**
 * 消息延迟枚举
 *
 * @author zhoushuai
 * @Date 2019/10/23 11:44
 */
@Getter
public enum DelayLevelEnum {
    L_1s(1, "1s" ),
    L_5s(2 , "5s" ),
    L_10s(3 , "10s" ),
    L_30s(4 , "30s" ),
    L_1m(5 , "1m" ),
    L_2m(6 , "2m" ),
    L_3m(7 , "3m" ),
    L_4m(8 , "4m" ),
    L_5m(9, "5m" ),
    L_6m(10, "6m" ),
    L_7m(11, "7m" ),
    L_8m(12, "8m" ),
    L_9m(13, "9m" ),
    L_10m(14, "10m" ),
    L_20m(15, "20m" ),
    L_30m(16 , "30m" ),
    L_1h(17 , "1h" ),
    L_2h(18 , "2h" ),
    ;

    private Integer code;
    private String codeDesc;

    DelayLevelEnum(Integer code, String codeDesc) {
        this.code = code;
        this.codeDesc = codeDesc;
    }

}
