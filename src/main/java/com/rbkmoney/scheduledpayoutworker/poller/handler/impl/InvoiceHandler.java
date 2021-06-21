package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceHandler implements PaymentProcessingHandler {

    private final ShopMetaDao shopMetaDao;

    private final InvoiceDao invoiceDao;

    private final PartyManagementService partyManagementService;

    private final Filter filter = new PathConditionFilter(
            new PathConditionRule(
                    "invoice_created",
                    new IsNullCondition().not()
            )
    );

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

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }

}
