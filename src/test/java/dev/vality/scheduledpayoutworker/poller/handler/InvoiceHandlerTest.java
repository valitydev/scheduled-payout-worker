package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoiceCreated;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.InvoiceHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvoiceHandlerTest {

    @Mock
    private ShopMetaDao shopMetaDao;

    @Mock
    private InvoiceDao invoiceDao;

    private InvoiceHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new InvoiceHandler(shopMetaDao, invoiceDao);
        preparedMocks = new Object[] {shopMetaDao, invoiceDao};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        assertTrue(handler.accept(prepareHandleInput(), prepareEvent()));
    }

    @Test
    void handleWithExistingPartyShop() {
        InvoiceChange change = prepareHandleInput();
        Invoice invoice = change.getInvoiceCreated().getInvoice();
        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setPartyId(invoice.getOwnerId());
        shopMeta.setShopId(invoice.getShopId());
        shopMeta.setHasPaymentInstitutionAccPayTool(true);
        when(shopMetaDao.get(invoice.getOwnerId(), invoice.getShopId())).thenReturn(shopMeta);
        handler.handle(change, null);
        verify(shopMetaDao, times(1)).get(invoice.getOwnerId(), invoice.getShopId());
        verify(invoiceDao, times(1))
                .save(eq(invoice.getId()), eq(invoice.getOwnerId()), eq(invoice.getShopId()), eq(null), any());
    }

    @Test
    void handleWithNonExistingPartyShop() {
        InvoiceChange change = prepareHandleInput();
        Invoice invoice = change.getInvoiceCreated().getInvoice();
        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setPartyId(invoice.getOwnerId());
        shopMeta.setShopId(invoice.getShopId());
        shopMeta.setHasPaymentInstitutionAccPayTool(false);
        when(shopMetaDao.get(invoice.getOwnerId(), invoice.getShopId())).thenReturn(shopMeta);
        handler.handle(change, null);
        verify(shopMetaDao, times(1)).get(invoice.getOwnerId(), invoice.getShopId());
    }

    private InvoiceChange prepareHandleInput() {
        InvoiceChange invoiceChange = fillTBaseObject(new InvoiceChange(), InvoiceChange.class);
        InvoiceCreated invoiceCreated = fillTBaseObject(new InvoiceCreated(), InvoiceCreated.class);
        Invoice invoice = fillTBaseObject(new Invoice(), Invoice.class);
        invoiceCreated.setInvoice(invoice);
        invoiceChange.setInvoiceCreated(invoiceCreated);
        return invoiceChange;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }


}