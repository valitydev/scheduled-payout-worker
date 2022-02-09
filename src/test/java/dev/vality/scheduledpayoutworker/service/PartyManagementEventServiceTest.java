package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.payment_processing.PartyChange;
import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import dev.vality.scheduledpayoutworker.service.impl.PartyManagementEventServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.mockito.Mockito.*;

class PartyManagementEventServiceTest {

    @Mock
    private PartyManagementHandler handler;

    private PartyManagementEventService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {handler};
        service = new PartyManagementEventServiceImpl(List.of(handler));
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void processEvent() {
        PartyEventData eventData = preparePartyEventData();
        MachineEvent machineEvent = prepareMachineEvent();
        PartyChange change = eventData.getChanges().get(0);
        when(handler.accept(change, machineEvent)).thenReturn(true);
        service.processEvent(machineEvent, eventData);
        verify(handler, times(1)).accept(change, machineEvent);
        verify(handler, times(1)).handle(change, machineEvent);
    }

    private PartyEventData preparePartyEventData() {
        PartyEventData eventData = fillTBaseObject(new PartyEventData(), PartyEventData.class);
        eventData.setChanges(List.of(fillTBaseObject(new PartyChange(), PartyChange.class)));
        return eventData;
    }

    private MachineEvent prepareMachineEvent() {
        return fillTBaseObject(new MachineEvent(), MachineEvent.class);
    }
}