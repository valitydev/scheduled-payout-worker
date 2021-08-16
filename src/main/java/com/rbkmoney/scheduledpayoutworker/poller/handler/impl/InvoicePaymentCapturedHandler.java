package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentCapturedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStatusChanged()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentStatusChanged().getStatus().isSetCaptured();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        LocalDateTime capturedAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        if (paymentDao.get(invoiceId, paymentId) == null) {
            log.debug("Payment not found, invoiceId='{}', paymentId='{}'",
                    invoiceId, paymentId);
            return;
        }

        paymentDao.markAsCaptured(eventId, invoiceId, paymentId, capturedAt);
        log.info("Payment have been captured, invoiceId={}, paymentId={}", invoiceId, paymentId);
    }

}
