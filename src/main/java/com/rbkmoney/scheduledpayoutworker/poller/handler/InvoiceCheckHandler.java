package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class InvoiceCheckHandler {

    private final InvoiceDao invoiceDao;

    protected boolean invoiceExists(String invoiceId) {
        Invoice invoice = invoiceDao.get(invoiceId);
        if (invoice == null) {
            log.debug("Invoice not found, invoiceId='{}'",
                    invoiceId);
            return false;
        }

        return true;
    }
}
