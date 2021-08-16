package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentCancelledHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStatusChanged()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentStatusChanged().getStatus().isSetCancelled();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        if (paymentDao.get(invoiceId, paymentId) == null) {
            log.debug("Payment not found, invoiceId='{}', paymentId='{}'",
                    invoiceId, paymentId);
            return;
        }

        paymentDao.markAsCancelled(eventId, invoiceId, paymentId);
        log.info("Payment have been cancelled, invoiceId={}, paymentId={}", invoiceId, paymentId);
    }

}
