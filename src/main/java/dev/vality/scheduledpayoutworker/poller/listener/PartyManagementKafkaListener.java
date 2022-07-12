package dev.vality.scheduledpayoutworker.poller.listener;

import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.kafka.common.util.LogUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.scheduledpayoutworker.service.PartyManagementEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PartyManagementKafkaListener {

    private final PartyManagementEventService partyManagementService;
    private final ConversionService converter;

    @KafkaListener(topics = "${kafka.topics.party-management.id}", containerFactory = "pmContainerFactory")
    public void handle(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) {
        log.info("Got partyManagement machineEvent batch with size: {}", messages.size());
        for (ConsumerRecord<String, SinkEvent> message : messages) {
            if (message != null && message.value().isSetEvent()) {
                MachineEvent machineEvent = message.value().getEvent();
                PartyEventData partyEventData = converter.convert(machineEvent, PartyEventData.class);
                partyManagementService.processEvent(machineEvent, partyEventData);
            }
        }
        ack.acknowledge();
        log.info("Batch partyManagement has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }
}