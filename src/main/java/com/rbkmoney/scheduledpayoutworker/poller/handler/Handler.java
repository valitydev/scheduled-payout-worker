package com.rbkmoney.scheduledpayoutworker.poller.handler;


import com.rbkmoney.machinegun.eventsink.MachineEvent;

public interface Handler<T, E> {

    boolean accept(T change, MachineEvent machineEvent);

    void handle(T change, E event);

}
