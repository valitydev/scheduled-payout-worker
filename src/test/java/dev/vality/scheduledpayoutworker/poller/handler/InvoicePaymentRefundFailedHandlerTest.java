package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.InvoicePaymentRefundFailed;
import dev.vality.damsel.domain.InvoicePaymentRefundStatus;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.RefundDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoicePaymentRefundFailedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentRefundFailedHandlerTest {

    @Mock
    private RefundDao refundDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoicePaymentRefundFailedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentRefundFailedHandler(refundDao, invoiceDao);
        preparedMocks = new Object[] {refundDao};
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
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange
                .getPayload()
                .getInvoicePaymentRefundChange();
        verify(refundDao, times(1)).markAsFailed(event.getEventId(), event.getSourceId(),
                invoicePaymentChange.getId(),
                invoicePaymentRefundChange.getId());
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
        refundStatus.setFailed(fillTBaseObject(new InvoicePaymentRefundFailed(), InvoicePaymentRefundFailed.class));
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}