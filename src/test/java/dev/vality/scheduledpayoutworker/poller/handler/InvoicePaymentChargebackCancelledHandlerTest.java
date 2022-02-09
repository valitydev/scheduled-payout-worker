package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.InvoicePaymentChargebackCancelled;
import dev.vality.damsel.domain.InvoicePaymentChargebackStatus;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.scheduledpayoutworker.dao.ChargebackDao;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoicePaymentChargebackCancelledHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentChargebackCancelledHandlerTest {

    @Mock
    private ChargebackDao chargebackDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoicePaymentChargebackCancelledHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentChargebackCancelledHandler(chargebackDao, invoiceDao);
        preparedMocks = new Object[] {chargebackDao};
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
        verify(chargebackDao, times(1))
                .markAsCancelled(event.getEventId(), event.getSourceId(), invoicePaymentChange.getId(),
                        invoicePaymentChange.getPayload().getInvoicePaymentChargebackChange().getId());

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
        InvoicePaymentChargebackChange chargebackChange =
                fillTBaseObject(new InvoicePaymentChargebackChange(), InvoicePaymentChargebackChange.class);
        invoicePaymentChangePayload.setInvoicePaymentChargebackChange(chargebackChange);
        InvoicePaymentChargebackChangePayload chargebackChangePayload =
                fillTBaseObject(new InvoicePaymentChargebackChangePayload(),
                        InvoicePaymentChargebackChangePayload.class);
        chargebackChange.setPayload(chargebackChangePayload);
        InvoicePaymentChargebackStatusChanged chargebackBodyStatusChanged =
                fillTBaseObject(new InvoicePaymentChargebackStatusChanged(),
                        InvoicePaymentChargebackStatusChanged.class);
        InvoicePaymentChargebackStatus chargebackStatus =
                fillTBaseObject(new InvoicePaymentChargebackStatus(), InvoicePaymentChargebackStatus.class);
        chargebackBodyStatusChanged.setStatus(chargebackStatus);
        chargebackStatus.setCancelled(
                fillTBaseObject(new InvoicePaymentChargebackCancelled(), InvoicePaymentChargebackCancelled.class));
        chargebackChange.setPayload(chargebackChangePayload);
        chargebackChangePayload.setInvoicePaymentChargebackStatusChanged(chargebackBodyStatusChanged);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}