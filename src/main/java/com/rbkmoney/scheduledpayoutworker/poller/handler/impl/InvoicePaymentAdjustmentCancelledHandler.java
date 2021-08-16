package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.AdjustmentDao;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentAdjustmentCancelledHandler implements PaymentProcessingHandler {

    private final AdjustmentDao adjustmentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && invoiceChange
                .getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange().getPayload()
                .isSetInvoicePaymentAdjustmentStatusChanged()
                && invoiceChange
                .getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange().getPayload()
                .getInvoicePaymentAdjustmentStatusChanged().getStatus().isSetCancelled()
                && invoiceDao.get(event.getSourceId()) != null;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();

        String adjustmentId = invoicePaymentAdjustmentChange.getId();

        adjustmentDao.markAsCancelled(eventId, invoiceId, paymentId, adjustmentId);
        log.info("Adjustment have been cancelled, invoiceId={}, paymentId={}, adjustmentId={}",
                invoiceId, paymentId, adjustmentId);
    }

}
