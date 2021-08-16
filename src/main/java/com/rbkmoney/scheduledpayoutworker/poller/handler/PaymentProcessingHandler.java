package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;

public interface PaymentProcessingHandler extends Handler<InvoiceChange, MachineEvent> {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentProcessingHandler.class);

    default boolean invoiceExists(String invoiceId) {
        Invoice invoice = getInvoiceDao().get(invoiceId);
        if (invoice == null) {
            log.debug("Invoice not found, invoiceId='{}'",
                    invoiceId);
            return false;
        }

        return true;
    }

    default InvoiceDao getInvoiceDao() {
        return null;
    }
}
