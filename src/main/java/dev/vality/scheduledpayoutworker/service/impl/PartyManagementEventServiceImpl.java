package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.payment_processing.PartyChange;
import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import dev.vality.scheduledpayoutworker.service.PartyManagementEventService;
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
    public void processEvent(MachineEvent event, PartyEventData eventPayload) throws NotFoundException {
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
