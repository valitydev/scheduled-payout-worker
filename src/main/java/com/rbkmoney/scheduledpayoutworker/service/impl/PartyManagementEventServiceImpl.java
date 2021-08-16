package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyManagementEventServiceImpl implements PartyManagementEventService {

    private final List<PartyManagementHandler> handlers;

    @Override
    public void processEvent(MachineEvent event, PartyEventData eventPayload)
            throws StorageException, NotFoundException {
        long eventId = event.getEventId();
        String createdAt = event.getCreatedAt();
        log.debug("Trying to save eventId, eventId={}, eventCreatedAt={}", eventId, createdAt);
        if (eventPayload != null && eventPayload.isSetChanges()) {
            for (PartyChange change : eventPayload.getChanges()) {
                handlers.stream()
                        .filter(handler -> handler.accept(change, event))
                        .forEach(handler -> handler.handle(change, event));
            }
            log.info("Event id have been saved, eventId={}, eventCreatedAt={}", eventId, createdAt);
        }
    }

}
