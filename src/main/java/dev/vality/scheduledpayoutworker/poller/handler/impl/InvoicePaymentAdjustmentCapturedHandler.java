package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.dao.AdjustmentDao;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentAdjustmentCapturedHandler implements PaymentProcessingHandler {

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
                .getInvoicePaymentAdjustmentStatusChanged().getStatus().isSetCaptured()
                && invoiceDao.get(event.getSourceId()) != null;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        LocalDateTime capturedAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();

        String adjustmentId = invoicePaymentAdjustmentChange.getId();

        adjustmentDao.markAsCaptured(eventId, invoiceId, paymentId, adjustmentId, capturedAt);
        log.info("Adjustment have been captured, invoiceId={}, paymentId={}, adjustmentId={}",
                invoiceId, paymentId, adjustmentId);
    }

}
