package com.rbkmoney.scheduledpayoutworker.poller.listener;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.scheduledpayoutworker.converter.SourceEventParser;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PartyManagementKafkaListener {

    private final PartyManagementEventService partyManagementService;
    private final SourceEventParser<PartyEventData> partyEventDataParser;

    @KafkaListener(topics = "${kafka.topics.party-management.id}", containerFactory = "pmContainerFactory")
    public void handle(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) {
        log.info("Got partyManagement machineEvent batch with size: {}", messages.size());
        for (ConsumerRecord<String, SinkEvent> message : messages) {
            if (message != null && message.value().isSetEvent()) {
                MachineEvent machineEvent = message.value().getEvent();
                PartyEventData partyEventData = partyEventDataParser.parseEvent(machineEvent);
                partyManagementService.processPayloadEvent(machineEvent, partyEventData);
            }
        }

        ack.acknowledge();
        log.info("Batch partyManagement has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }
}