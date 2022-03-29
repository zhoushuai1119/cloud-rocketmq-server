package com.cloud.platform.rocketmq.exception;

public class DiscardOldJobException extends Exception {
    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public DiscardOldJobException(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return "discard job for too old timestamp: " + this.timestamp;
    }

}
