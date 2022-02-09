package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.InvoicePaymentCancelled;
import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChangePayload;
import dev.vality.damsel.payment_processing.InvoicePaymentStatusChanged;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoicePaymentCancelledHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentCancelledHandlerTest {

    @Mock
    private PaymentDao paymentDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoicePaymentCancelledHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentCancelledHandler(paymentDao, invoiceDao);
        preparedMocks = new Object[] {paymentDao};
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
        verify(paymentDao, times(1))
                .markAsCancelled(event.getEventId(), event.getSourceId(), change.getInvoicePaymentChange().getId());

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
        InvoicePaymentStatusChanged statusChanged =
                fillTBaseObject(new InvoicePaymentStatusChanged(), InvoicePaymentStatusChanged.class);
        invoicePaymentChangePayload.setInvoicePaymentStatusChanged(statusChanged);
        InvoicePaymentStatus
                invoicePaymentStatus =
                fillTBaseObject(new InvoicePaymentStatus(), InvoicePaymentStatus.class);
        statusChanged.setStatus(invoicePaymentStatus);
        InvoicePaymentCancelled cancelled =
                fillTBaseObject(new InvoicePaymentCancelled(), InvoicePaymentCancelled.class);
        invoicePaymentStatus.setCancelled(cancelled);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}