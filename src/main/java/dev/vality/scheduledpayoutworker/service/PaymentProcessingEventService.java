package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.exception.StorageException;

public interface PaymentProcessingEventService {

    void processEvent(MachineEvent machineEvent, EventPayload eventPayload) throws StorageException, NotFoundException;

}
