package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.InvoicePaymentChargebackRejected;
import com.rbkmoney.damsel.domain.InvoicePaymentChargebackStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.ChargebackDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.InvoicePaymentChargebackRejectedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.rbkmoney.scheduledpayoutworker.integration.data.TestData.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentChargebackRejectedHandlerTest {

    @Mock
    private ChargebackDao chargebackDao;

    private InvoicePaymentChargebackRejectedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentChargebackRejectedHandler(chargebackDao);
        preparedMocks = new Object[] {chargebackDao};
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
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();

        handler.handle(change, event);
        verify(chargebackDao, times(1))
                .markAsRejected(event.getEventId(), event.getSourceId(), invoicePaymentChange.getId(),
                        invoicePaymentChargebackChange.getId());
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
        InvoicePaymentChargebackStatusChanged statusChanged =
                fillTBaseObject(new InvoicePaymentChargebackStatusChanged(),
                        InvoicePaymentChargebackStatusChanged.class);
        chargebackChangePayload.setInvoicePaymentChargebackStatusChanged(statusChanged);
        InvoicePaymentChargebackStatus status = fillTBaseObject(new InvoicePaymentChargebackStatus(),
                InvoicePaymentChargebackStatus.class);
        statusChanged.setStatus(status);

        status.setRejected(fillTBaseObject(new InvoicePaymentChargebackRejected(),
                InvoicePaymentChargebackRejected.class));
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}