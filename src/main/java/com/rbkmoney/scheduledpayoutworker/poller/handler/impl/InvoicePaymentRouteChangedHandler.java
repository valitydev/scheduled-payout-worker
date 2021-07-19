package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.PaymentRoute;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentRouteChangedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRouteChanged();
    }

    @Override
    public void handle(InvoiceChange change, MachineEvent event) {
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String invoiceId = event.getSourceId();
        String paymentId = invoicePaymentChange.getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            log.debug("Invoice on payment not found, invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
            return;
        }

        PaymentRoute paymentRoute = invoicePaymentChange.getPayload().getInvoicePaymentRouteChanged().getRoute();
        payment.setProviderId(paymentRoute.getProvider().getId());
        payment.setTerminalId(paymentRoute.getTerminal().getId());

        paymentDao.save(payment);
        log.info("Payment route have been saved, route='{}', invoiceId='{}', paymentId='{}'",
                paymentRoute, invoiceId, paymentId);
    }

}