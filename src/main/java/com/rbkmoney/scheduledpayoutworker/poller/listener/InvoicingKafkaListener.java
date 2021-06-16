package com.rbkmoney.scheduledpayoutworker.poller.listener;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.converter.BinaryConverter;
import com.rbkmoney.scheduledpayoutworker.converter.impl.EventPayloadConverter;
import com.rbkmoney.scheduledpayoutworker.service.PaymentProcessingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@RequiredArgsConstructor
public class InvoicingKafkaListener {

    private final PaymentProcessingEventService paymentProcessingEventService;
    private final EventPayloadConverter parser;

    @KafkaListener(topics = "${kafka.topics.invoice.id}", containerFactory = "invContainerFactory")
    public void handle(MachineEvent event, Acknowledgment ack) {
        log.debug("Reading sinkEvent, sourceId: {}, sequenceId: {}", event.getSourceId(), event.getEventId());
        EventPayload payload = parser.convert(event.getData().getBin());
        if (payload.isSetInvoiceChanges()) {
            paymentProcessingEventService.processEvent(event, payload);
        }
        ack.acknowledge();
    }

}
