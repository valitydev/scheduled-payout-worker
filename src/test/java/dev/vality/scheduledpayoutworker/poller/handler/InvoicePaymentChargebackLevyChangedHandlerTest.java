package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Chargeback;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.scheduledpayoutworker.dao.ChargebackDao;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoicePaymentChargebackLevyChangedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoicePaymentChargebackLevyChangedHandlerTest {

    @Mock
    private ChargebackDao chargebackDao;

    @Mock
    private InvoiceDao invoiceDao;

    @Captor
    private ArgumentCaptor<Chargeback> chargebackArgumentCaptor;

    private InvoicePaymentChargebackLevyChangedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentChargebackLevyChangedHandler(chargebackDao, invoiceDao);
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

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();

        Chargeback chargeback = new Chargeback();

        when(chargebackDao
                .get(event.getSourceId(), invoicePaymentChange.getId(), invoicePaymentChargebackChange.getId()))
                .thenReturn(chargeback);
        handler.handle(change, event);
        verify(chargebackDao, times(1))
                .get(event.getSourceId(), invoicePaymentChange.getId(), invoicePaymentChargebackChange.getId());
        verify(chargebackDao, times(1)).save(chargebackArgumentCaptor.capture());

        Cash levy =
                invoicePaymentChargebackChange.getPayload().getInvoicePaymentChargebackLevyChanged().getLevy();
        Chargeback savedChargeback = chargebackArgumentCaptor.getValue();
        assertAll(
                () -> assertEquals(levy.getAmount(), savedChargeback.getLevyAmount()),
                () -> assertEquals(levy.getCurrency().getSymbolicCode(), savedChargeback.getLevyCurrencyCode())
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
        InvoicePaymentChargebackLevyChanged levyChanged =
                fillTBaseObject(new InvoicePaymentChargebackLevyChanged(),
                        InvoicePaymentChargebackLevyChanged.class);
        chargebackChangePayload.setInvoicePaymentChargebackLevyChanged(levyChanged);
        Cash cash = fillTBaseObject(new Cash(),
                Cash.class);
        levyChanged.setLevy(cash);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}