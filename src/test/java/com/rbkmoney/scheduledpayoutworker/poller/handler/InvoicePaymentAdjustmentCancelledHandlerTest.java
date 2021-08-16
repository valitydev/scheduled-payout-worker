package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentCancelled;
import com.rbkmoney.damsel.domain.InvoicePaymentAdjustmentStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.scheduledpayoutworker.dao.AdjustmentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.InvoicePaymentAdjustmentCancelledHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentAdjustmentCancelledHandlerTest {

    @Mock
    private AdjustmentDao adjustmentDao;

    private InvoicePaymentAdjustmentCancelledHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentAdjustmentCancelledHandler(adjustmentDao);
        preparedMocks = new Object[] {adjustmentDao};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        assertTrue(handler.accept(invoiceChange()));
    }

    @Test
    void handle() {
        InvoiceChange change = invoiceChange();
        MachineEvent event = prepareEvent();
        when(adjustmentDao
                .get(event.getSourceId(), change.getInvoicePaymentChange().getId(),
                        change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getId()))
                .thenReturn(new Adjustment());
        handler.handle(change, event);
        verify(adjustmentDao, times(1))
                .get(event.getSourceId(), change.getInvoicePaymentChange().getId(),
                        change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getId());
        verify(adjustmentDao, times(1))
                .markAsCancelled(event.getEventId(), event.getSourceId(), change.getInvoicePaymentChange().getId(),
                        change.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange().getId());
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
        InvoicePaymentAdjustmentCancelled cancelled =
                fillTBaseObject(new InvoicePaymentAdjustmentCancelled(), InvoicePaymentAdjustmentCancelled.class);
        status.setCancelled(cancelled);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }


}