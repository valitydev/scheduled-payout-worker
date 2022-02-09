package dev.vality.scheduledpayoutworker.poller.handler;


import dev.vality.machinegun.eventsink.MachineEvent;

public interface Handler<T, E> {

    boolean accept(T change, MachineEvent machineEvent);

    void handle(T change, E event);

}
