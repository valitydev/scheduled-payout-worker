package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.ChargebackDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentChargebackSuccededHandler implements PaymentProcessingHandler {


    private final ChargebackDao chargebackDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .isSetInvoicePaymentChargebackChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentChargebackChange().getPayload()
                .isSetInvoicePaymentChargebackStatusChanged()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentChargebackChange().getPayload()
                .getInvoicePaymentChargebackStatusChanged().getStatus().isSetAccepted();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange
                .getPayload()
                .getInvoicePaymentChargebackChange();

        String chargebackId = invoicePaymentChargebackChange.getId();

        if (chargebackDao.get(invoiceId, paymentId, chargebackId) == null) {
            log.debug("Invoice chargeback not found, invoiceId='{}', paymentId='{}', chargebackId='{}'",
                    invoiceId, paymentId, chargebackId);
            return;
        }

        LocalDateTime succeededAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        chargebackDao.markAsAccepted(eventId, invoiceId, paymentId, chargebackId, succeededAt);
        log.info("Chargeback have been accepted, invoiceId={}, paymentId={}, refundId={}",
                invoiceId, paymentId, chargebackId);
    }

}
