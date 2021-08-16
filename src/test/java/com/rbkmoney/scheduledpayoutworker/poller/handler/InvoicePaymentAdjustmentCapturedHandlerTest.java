package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentCaptured;
import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.scheduledpayoutworker.dao.AdjustmentDao;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.InvoicePaymentAdjustmentCapturedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentAdjustmentCapturedHandlerTest {

    @Mock
    private AdjustmentDao adjustmentDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoicePaymentAdjustmentCapturedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentAdjustmentCapturedHandler(adjustmentDao, invoiceDao);
        preparedMocks = new Object[] {adjustmentDao};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        MachineEvent event = prepareEvent();
        when(invoiceDao
                .get(event.getSourceId()))
                .thenReturn(new Invoice());
        assertTrue(handler.accept(invoiceChange(), event));
        verify(invoiceDao, times(1))
                .get(event.getSourceId());
    }

    @Test
    void handle() {
        InvoiceChange change = invoiceChange();
        MachineEvent event = prepareEvent();
        handler.handle(change, event);
        verify(adjustmentDao, times(1))
                .markAsCaptured(eq(event.getEventId()),
                        eq(event.getSourceId()),
                        eq(change.getInvoicePaymentChange().getId()),
                        eq(change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getId()),
                        any());
    }


    private InvoiceChange invoiceChange() {
        InvoiceChange invoiceChange = fillTBaseObject(new InvoiceChange(), InvoiceChange.class);
        InvoicePaymentChange invoicePaymentChange =
                fillTBaseObject(new InvoicePaymentChange(), InvoicePaymentChange.class);
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);
        InvoicePaymentChangePayload
                invoicePaymentChangePayload =
                fillTBaseObject(new InvoicePaymentChangePayload(), InvoicePaymentChangePayload.class);
        invoicePaymentChange.setPayload(invoicePaymentChangePayload);
        InvoicePaymentAdjustmentChange adjustmentChange =
                fillTBaseObject(new InvoicePaymentAdjustmentChange(), InvoicePaymentAdjustmentChange.class);
        invoicePaymentChangePayload.setInvoicePaymentAdjustmentChange(adjustmentChange);
        InvoicePaymentAdjustmentChangePayload payload = fillTBaseObject(new InvoicePaymentAdjustmentChangePayload(),
                InvoicePaymentAdjustmentChangePayload.class);
        adjustmentChange.setPayload(payload);

        InvoicePaymentAdjustmentStatusChanged statusChanged =
                fillTBaseObject(new InvoicePaymentAdjustmentStatusChanged(),
                        InvoicePaymentAdjustmentStatusChanged.class);
        payload.setInvoicePaymentAdjustmentStatusChanged(statusChanged);
        InvoicePaymentAdjustmentStatus status =
                fillTBaseObject(new InvoicePaymentAdjustmentStatus(), InvoicePaymentAdjustmentStatus.class);
        statusChanged.setStatus(status);
        InvoicePaymentAdjustmentCaptured captured =
                fillTBaseObject(new InvoicePaymentAdjustmentCaptured(), InvoicePaymentAdjustmentCaptured.class);
        status.setCaptured(captured);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }


}