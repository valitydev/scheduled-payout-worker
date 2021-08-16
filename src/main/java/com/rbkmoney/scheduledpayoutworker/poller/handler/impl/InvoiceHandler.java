package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
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
