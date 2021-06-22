package com.rbkmoney.scheduledpayoutworker.poller.handler;


public interface Handler<T, E> {

    boolean accept(T change);

    void handle(T change, E event);

}
