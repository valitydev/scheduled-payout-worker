package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.scheduledpayoutworker.dao.ChargebackDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.InvoicePaymentChargebackBodyChangedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoicePaymentChargebackBodyChangedHandlerTest {

    @Mock
    private ChargebackDao chargebackDao;

    @Captor
    private ArgumentCaptor<Chargeback> updatedChargebackCaptor;

    private InvoicePaymentChargebackBodyChangedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentChargebackBodyChangedHandler(chargebackDao);
        preparedMocks = new Object[] {chargebackDao};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        assertTrue(handler.accept(invoiceChange(), prepareEvent()));
    }

    @Test
    void handle() {
        InvoiceChange change = invoiceChange();
        MachineEvent event = prepareEvent();

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();

        Chargeback chargeback = new Chargeback();

        when(chargebackDao.get(event.getSourceId(), invoicePaymentChange.getId(),
                invoicePaymentChange.getPayload().getInvoicePaymentChargebackChange().getId())).thenReturn(chargeback);

        handler.handle(change, event);
        verify(chargebackDao, times(1))
                .get(event.getSourceId(), invoicePaymentChange.getId(),
                        invoicePaymentChange.getPayload().getInvoicePaymentChargebackChange().getId());
        verify(chargebackDao, times(1))
                .save(updatedChargebackCaptor.capture());

        Cash cash =
                invoicePaymentChange.getPayload()
                        .getInvoicePaymentChargebackChange().getPayload()
                        .getInvoicePaymentChargebackBodyChanged().getBody();

        Chargeback updatedChargeback = updatedChargebackCaptor.getValue();

        assertAll(
                () -> assertEquals(cash.getAmount(), updatedChargeback.getAmount()),
                () -> assertEquals(cash.getCurrency().getSymbolicCode(), updatedChargeback.getCurrencyCode())
        );

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
        InvoicePaymentChargebackBodyChanged chargebackBodyChanged =
                fillTBaseObject(new InvoicePaymentChargebackBodyChanged(), InvoicePaymentChargebackBodyChanged.class);
        chargebackChange.setPayload(chargebackChangePayload);
        chargebackChangePayload.setInvoicePaymentChargebackBodyChanged(chargebackBodyChanged);

        Cash cash = fillTBaseObject(new Cash(), Cash.class);
        chargebackBodyChanged.setBody(cash);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}