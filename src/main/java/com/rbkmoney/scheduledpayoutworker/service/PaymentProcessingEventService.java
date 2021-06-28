package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;

public interface PaymentProcessingEventService {

    void processEvent(MachineEvent machineEvent, EventPayload eventPayload) throws StorageException, NotFoundException;

}
