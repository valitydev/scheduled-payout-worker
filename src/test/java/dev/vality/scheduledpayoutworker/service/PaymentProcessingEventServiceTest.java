package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import dev.vality.scheduledpayoutworker.service.impl.PaymentProcessingEventServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.mockito.Mockito.*;

class PaymentProcessingEventServiceTest {

    @Mock
    private PaymentProcessingHandler handler;

    private PaymentProcessingEventService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {handler};
        service = new PaymentProcessingEventServiceImpl(List.of(handler));
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void processEvent() {
        EventPayload eventData = prepareEventPayload();
        MachineEvent machineEvent = prepareMachineEvent();
        InvoiceChange change = eventData.getInvoiceChanges().get(0);
        when(handler.accept(change, machineEvent)).thenReturn(true);
        service.processEvent(machineEvent, eventData);
        verify(handler, times(1)).accept(change, machineEvent);
        verify(handler, times(1)).handle(change, machineEvent);
    }

    private EventPayload prepareEventPayload() {
        EventPayload eventData = fillTBaseObject(new EventPayload(), EventPayload.class);
        eventData.setInvoiceChanges(List.of(fillTBaseObject(new InvoiceChange(), InvoiceChange.class)));
        return eventData;
    }

    private MachineEvent prepareMachineEvent() {
        return fillTBaseObject(new MachineEvent(), MachineEvent.class);
    }
}