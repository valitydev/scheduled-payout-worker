package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.exception.StorageException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import dev.vality.scheduledpayoutworker.service.PaymentProcessingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessingEventServiceImpl implements PaymentProcessingEventService {

    private final List<PaymentProcessingHandler> handlers;

    @Override
    public void processEvent(MachineEvent machineEvent, EventPayload eventPayload)
            throws StorageException, NotFoundException {
        if (eventPayload.isSetInvoiceChanges()) {
            log.info("Trying to save event, sourceId={}, eventId={}, eventCreatedAt={}",
                    machineEvent.getSourceId(), machineEvent.getEventId(), machineEvent.getCreatedAt());
            for (InvoiceChange change : eventPayload.getInvoiceChanges()) {
                PaymentProcessingHandler handler = getHandler(change, machineEvent);
                if (handler != null) {
                    log.debug("Trying to handle change, change='{}', sourceId='{}', eventId='{}'",
                            change, machineEvent.getSourceId(), machineEvent.getEventId());
                    try {
                        handler.handle(change, machineEvent);
                        log.info("Change have been handled, change='{}', sourceId='{}', eventId='{}'",
                                change, machineEvent.getSourceId(), machineEvent.getEventId());
                    } catch (DaoException ex) {
                        throw new StorageException(
                                String.format("Failed to save event, change='%s', sourceId='%s', eventId='%d'",
                                        change, machineEvent.getSourceId(), machineEvent.getEventId()), ex);
                    }
                }
            }
        }
    }

    private PaymentProcessingHandler getHandler(InvoiceChange change, MachineEvent machineEvent) {
        for (PaymentProcessingHandler handler : handlers) {
            if (handler.accept(change, machineEvent)) {
                return handler;
            }
        }
        return null;
    }
}
