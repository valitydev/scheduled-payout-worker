package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.exception.StorageException;

public interface PartyManagementEventService {

    void processEvent(MachineEvent machineEvent, PartyEventData eventPayload)
            throws StorageException, NotFoundException;

}
