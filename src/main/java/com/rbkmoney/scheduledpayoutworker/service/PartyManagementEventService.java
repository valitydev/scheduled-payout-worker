package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;

public interface PartyManagementEventService {

    void processPayloadEvent(MachineEvent machineEvent, PartyEventData eventPayload)
            throws StorageException, NotFoundException;

}
