package dev.vality.scheduledpayoutworker.poller.listener;

import dev.vality.damsel.payment_processing.PartyChange;
import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.scheduledpayoutworker.service.PartyManagementEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class PartyManagementKafkaListenerTest {

    @Mock
    private PartyManagementEventService partyManagementEventService;
    @Mock
    private ConversionService converter;
    @Mock
    private Acknowledgment ack;

    private PartyManagementKafkaListener listener;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new PartyManagementKafkaListener(partyManagementEventService, converter);
    }

    @AfterEach
    public void clean() throws Exception {
        mocks.close();
    }

    @Test
    void listenEmptyPartyManagementException() {
        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        Mockito.when(converter.convert(message, PartyEventData.class)).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)),
                ack));

        Mockito.verify(ack, Mockito.times(0)).acknowledge();
    }

    @Test
    void listenValidPartyManagement() {
        PartyEventData partyEventData = new PartyEventData();
        ArrayList<PartyChange> partyChanges = new ArrayList<>();
        partyChanges.add(new PartyChange());
        partyEventData.setChanges(partyChanges);
        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);

        Mockito.when(converter.convert(message, PartyEventData.class)).thenReturn(partyEventData);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)),
                ack);

        Mockito.verify(partyManagementEventService, Mockito.times(1)).processEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }
}
