package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentCaptureStarted;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentCaptureStartedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange()
                .getPayload().isSetInvoicePaymentCaptureStarted()
                && invoiceDao.get(event.getSourceId()) != null;
    }


    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            log.debug("Invoice payment not found, invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
            return;
        }
        InvoicePaymentCaptureStarted invoicePaymentCaptureStarted = invoiceChange.getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentCaptureStarted();
        if (invoicePaymentCaptureStarted.getParams().isSetCash()) {
            payment.setAmount(invoicePaymentCaptureStarted.getParams().getCash().getAmount());
            payment.setCurrencyCode(invoicePaymentCaptureStarted.getParams().getCash().getCurrency().getSymbolicCode());
        }
        paymentDao.save(payment);
        log.info("Payment capture started have been saved invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
    }

}
