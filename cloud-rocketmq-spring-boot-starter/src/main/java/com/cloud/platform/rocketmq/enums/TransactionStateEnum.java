package com.cloud.platform.rocketmq.enums;

import lombok.Getter;

import java.util.HashMap;

/**
 * @Author 马腾飞
 * @Date 2019/8/29
 * @Time 12:28
 * @Description
 */
@Getter
public enum TransactionStateEnum {
    COMMIT_MESSAGE(1, "COMMIT_MESSAGE"),
    ROLLBACK_MESSAGE(2, "ROLLBACK_MESSAGE"),
    UNKNOW(3, "待检查"),
    ;

    private Integer code;
    private String message;

    TransactionStateEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private static final HashMap<Integer, TransactionStateEnum> map = new HashMap<>();

    static {
        for (TransactionStateEnum status : TransactionStateEnum.values()) {
            map.put(status.getCode(), status);
        }
    }

    public static TransactionStateEnum of(int code) {
        return map.get(code);
    }

}
