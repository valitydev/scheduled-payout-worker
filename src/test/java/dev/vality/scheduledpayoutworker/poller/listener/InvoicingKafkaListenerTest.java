package dev.vality.scheduledpayoutworker.poller.listener;

import dev.vality.damsel.payment_processing.Event;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.scheduledpayoutworker.service.PaymentProcessingEventService;
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

class InvoicingKafkaListenerTest {

    @Mock
    private PaymentProcessingEventService paymentProcessingEventService;
    @Mock
    private ConversionService converter;
    @Mock
    private Acknowledgment ack;

    private InvoicingKafkaListener listener;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        listener = new InvoicingKafkaListener(paymentProcessingEventService, converter);
    }

    @AfterEach
    public void clean() throws Exception {
        mocks.close();
    }

    @Test
    void listenNonInvoiceChanges() {

        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);
        EventPayload payload = new EventPayload();
        payload.setCustomerChanges(List.of());
        Mockito.when(converter.convert(message, EventPayload.class)).thenReturn(payload);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)
        ), ack);

        Mockito.verify(paymentProcessingEventService, Mockito.times(0)).processEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

    @Test
    void listenEmptyException() {
        MachineEvent message = new MachineEvent();
        message.setData(new Value());
        message.getData().setBin(new byte[0]);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        Mockito.when(converter.convert(message, EventPayload.class)).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)
        ), ack));

        Mockito.verify(ack, Mockito.times(0)).acknowledge();
    }

    @Test
    void listenChanges() {
        Event event = new Event();
        EventPayload payload = new EventPayload();
        ArrayList<InvoiceChange> invoiceChanges = new ArrayList<>();
        invoiceChanges.add(new InvoiceChange());
        payload.setInvoiceChanges(invoiceChanges);
        event.setPayload(payload);
        MachineEvent message = new MachineEvent();
        Mockito.when(converter.convert(message, EventPayload.class)).thenReturn(payload);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(List.of(
                new ConsumerRecord<>("Test", 0, 0, "", sinkEvent)
        ), ack);

        Mockito.verify(paymentProcessingEventService, Mockito.times(1)).processEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

}
