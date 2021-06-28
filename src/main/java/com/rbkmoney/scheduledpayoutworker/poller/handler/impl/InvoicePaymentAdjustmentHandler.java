package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustment;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.enums.AdjustmentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.AdjustmentDao;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentAdjustmentHandler implements PaymentProcessingHandler {

    private final AdjustmentDao adjustmentDao;

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && invoiceChange
                .getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange().getPayload()
                .isSetInvoicePaymentAdjustmentCreated();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();

        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();
        InvoicePaymentAdjustment invoicePaymentAdjustment = invoicePaymentAdjustmentChange
                .getPayload().getInvoicePaymentAdjustmentCreated().getAdjustment();

        Adjustment adjustment = new Adjustment();
        adjustment.setEventId(eventId);
        adjustment.setInvoiceId(invoiceId);
        adjustment.setPaymentId(paymentId);

        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            log.debug("Payment on adjustment not found, invoiceId='{}', paymentId='{}', adjustmentId='{}'",
                    invoiceId, paymentId, invoicePaymentAdjustment.getId());
            return;
        }

        adjustment.setPartyId(payment.getPartyId());
        adjustment.setShopId(payment.getShopId());

        adjustment.setAdjustmentId(invoicePaymentAdjustment.getId());
        adjustment.setStatus(AdjustmentStatus.PENDING);
        adjustment.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentAdjustment.getCreatedAt()));

        Long oldAmount = DamselUtil.computeMerchantAmount(invoicePaymentAdjustment.getOldCashFlowInverse());
        Long newAmount = DamselUtil.computeMerchantAmount(invoicePaymentAdjustment.getNewCashFlow());
        Long amount = oldAmount + newAmount;
        adjustment.setAmount(amount);

        adjustmentDao.save(adjustment);
        log.info("Adjustment have been saved, adjustment={}", adjustment);
    }

}
