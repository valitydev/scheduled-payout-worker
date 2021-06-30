package com.rbkmoney.scheduledpayoutworker.exception;

public class JobExecutionException extends RuntimeException {

    public JobExecutionException() {
    }

    public JobExecutionException(String message) {
        super(message);
    }

    public JobExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobExecutionException(Throwable cause) {
        super(cause);
    }

}
