package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.InvoicePaymentAdjustment;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.enums.AdjustmentStatus;
import dev.vality.payouter.domain.tables.pojos.Adjustment;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.dao.AdjustmentDao;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import dev.vality.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentAdjustmentHandler implements PaymentProcessingHandler {

    private final AdjustmentDao adjustmentDao;

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && invoiceChange
                .getInvoicePaymentChange().getPayload()
                .getInvoicePaymentAdjustmentChange().getPayload()
                .isSetInvoicePaymentAdjustmentCreated()
                && invoiceDao.get(event.getSourceId()) != null;
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

        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(
                    String.format("Payment on adjustment not found, invoiceId='%s', paymentId='%s', adjustmentId='%s'",
                            invoiceId, paymentId, invoicePaymentAdjustment.getId()));
        }

        Adjustment adjustment = new Adjustment();
        adjustment.setEventId(eventId);
        adjustment.setInvoiceId(invoiceId);
        adjustment.setPaymentId(paymentId);
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
