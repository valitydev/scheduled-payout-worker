package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceHandler implements PaymentProcessingHandler {

    private final ShopMetaDao shopMetaDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoiceCreated();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) throws DaoException {
        Invoice invoice = invoiceChange.getInvoiceCreated().getInvoice();
        ShopMeta shopMeta = shopMetaDao.get(invoice.getOwnerId(), invoice.getShopId());

        if (shopMeta != null && shopMeta.getHasPaymentInstitutionAccPayTool()) {
            invoiceDao.save(
                    invoice.getId(),
                    invoice.getOwnerId(),
                    invoice.getShopId(),
                    invoice.isSetPartyRevision() ? invoice.getPartyRevision() : null,
                    TypeUtil.stringToLocalDateTime(invoice.getCreatedAt())
            );
            log.info("Invoice have been saved, invoiceId={}, partyId={}, shopId={}",
                    invoice.getId(), invoice.getOwnerId(), invoice.getShopId());
        }
    }

}
