package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentCaptureStarted;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InvoicePaymentCaptureStartedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentCaptureStartedHandler(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_capture_started",
                new IsNullCondition().not()));
    }


    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            log.warn("Invoice payment not found, invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
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

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
