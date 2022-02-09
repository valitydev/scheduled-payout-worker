package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.dao.AdjustmentDao;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoicePaymentAdjustmentHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoicePaymentAdjustmentHandlerTest {

    @Mock
    private AdjustmentDao adjustmentDao;

    @Mock
    private PaymentDao paymentDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoicePaymentAdjustmentHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoicePaymentAdjustmentHandler(adjustmentDao, paymentDao, invoiceDao);
        preparedMocks = new Object[] {adjustmentDao, paymentDao};
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
        String invoiceId = event.getSourceId();
        String paymentId = change.getInvoicePaymentChange().getId();
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();

        Payment payment = new Payment();
        payment.setPartyId(partyId);
        payment.setShopId(shopId);

        when(paymentDao.get(invoiceId, paymentId)).thenReturn(payment);
        handler.handle(change, event);
        verify(paymentDao, times(1))
                .get(event.getSourceId(), change.getInvoicePaymentChange().getId());

        verify(adjustmentDao, times(1)).save(notNull());

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
        InvoicePaymentAdjustmentCreated adjustmentCreated =
                fillTBaseObject(new InvoicePaymentAdjustmentCreated(),
                        InvoicePaymentAdjustmentCreated.class);
        payload.setInvoicePaymentAdjustmentCreated(adjustmentCreated);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}