package com.rbkmoney.scheduledpayoutworker.kafka;

import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.machinegun.msgpack.Value;
import com.rbkmoney.scheduledpayoutworker.converter.impl.PartyEventConverter;
import com.rbkmoney.scheduledpayoutworker.poller.listener.PartyManagementKafkaListener;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

public class PartyManagementListenerTest {

    @Mock
    private PartyManagementEventService partyManagementEventService;
    @Mock
    private PartyEventConverter parser;
    @Mock
    private Acknowledgment ack;

    private PartyManagementKafkaListener listener;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new PartyManagementKafkaListener(partyManagementEventService, parser);
    }

    @AfterEach
    public void clean() throws Exception {
        mocks.close();
    }

    @Test
    public void listenEmptyPartyManagementException() {
        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        Mockito.when(parser.convert(message.getData().getBin())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)),
                ack));

        Mockito.verify(ack, Mockito.times(0)).acknowledge();
    }

    @Test
    public void listenValidPartyManagement() {
        PartyEventData partyEventData = new PartyEventData();
        ArrayList<PartyChange> partyChanges = new ArrayList<>();
        partyChanges.add(new PartyChange());
        partyEventData.setChanges(partyChanges);
        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);

        Mockito.when(parser.convert(message.getData().getBin())).thenReturn(partyEventData);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)),
                ack);

        Mockito.verify(partyManagementEventService, Mockito.times(1)).processPayloadEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }
}
