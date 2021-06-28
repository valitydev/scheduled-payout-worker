package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus;
import com.rbkmoney.damsel.domain.InvoicePaymentRefundSucceeded;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.RefundDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.InvoicePaymentRefundSucceededHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentRefundSucceededHandlerTest {

    @Mock
    private RefundDao refundDao;

    private InvoicePaymentRefundSucceededHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentRefundSucceededHandler(refundDao);
        preparedMocks = new Object[] {refundDao};
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

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange
                .getPayload()
                .getInvoicePaymentRefundChange();

        handler.handle(change, event);
        verify(refundDao, times(1))
                .markAsSucceeded(eq(event.getEventId()), eq(event.getSourceId()), eq(invoicePaymentChange.getId()),
                        eq(invoicePaymentRefundChange.getId()), any());
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

        InvoicePaymentRefundChange refundChange =
                fillTBaseObject(new InvoicePaymentRefundChange(), InvoicePaymentRefundChange.class);
        invoicePaymentChangePayload.setInvoicePaymentRefundChange(refundChange);
        InvoicePaymentRefundChangePayload refundChangePayload =
                fillTBaseObject(new InvoicePaymentRefundChangePayload(), InvoicePaymentRefundChangePayload.class);
        refundChange.setPayload(refundChangePayload);
        InvoicePaymentRefundStatusChanged statusChanged =
                fillTBaseObject(new InvoicePaymentRefundStatusChanged(), InvoicePaymentRefundStatusChanged.class);
        refundChangePayload.setInvoicePaymentRefundStatusChanged(statusChanged);
        InvoicePaymentRefundStatus refundStatus =
                fillTBaseObject(new InvoicePaymentRefundStatus(), InvoicePaymentRefundStatus.class);
        statusChanged.setStatus(refundStatus);
        refundStatus.setSucceeded(
                fillTBaseObject(new InvoicePaymentRefundSucceeded(), InvoicePaymentRefundSucceeded.class));
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}