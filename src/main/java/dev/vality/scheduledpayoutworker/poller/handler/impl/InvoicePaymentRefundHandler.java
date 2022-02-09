package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.InvoicePaymentRefund;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentRefundChange;
import dev.vality.damsel.payment_processing.InvoicePaymentRefundCreated;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.enums.RefundStatus;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.payouter.domain.tables.pojos.Refund;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.dao.RefundDao;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import dev.vality.scheduledpayoutworker.util.CashFlowType;
import dev.vality.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static dev.vality.scheduledpayoutworker.util.CashFlowType.FEE;
import static dev.vality.scheduledpayoutworker.util.CashFlowType.RETURN_FEE;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentRefundHandler implements PaymentProcessingHandler {

    private final RefundDao refundDao;

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .isSetInvoicePaymentRefundChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentRefundChange().getPayload().isSetInvoicePaymentRefundCreated()
                && invoiceDao.get(event.getSourceId()) != null;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange
                .getPayload()
                .getInvoicePaymentRefundChange();

        InvoicePaymentRefundCreated invoicePaymentRefundCreated = invoicePaymentRefundChange
                .getPayload()
                .getInvoicePaymentRefundCreated();

        InvoicePaymentRefund invoicePaymentRefund = invoicePaymentRefundCreated.getRefund();

        Refund refund = new Refund();
        refund.setEventId(eventId);

        Payment payment = paymentDao.get(invoiceId, paymentId);

        if (payment == null) {
            throw new NotFoundException(
                    String.format("Payment on refund not found, invoiceId='%s', paymentId='%s', refundId='%s'",
                            invoiceId, paymentId, invoicePaymentRefund.getId()));
        }

        refund.setPartyId(payment.getPartyId());
        refund.setShopId(payment.getShopId());

        refund.setInvoiceId(invoiceId);
        refund.setPaymentId(paymentId);
        refund.setRefundId(invoicePaymentRefund.getId());
        refund.setStatus(RefundStatus.PENDING);
        refund.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentRefund.getCreatedAt()));
        refund.setReason(invoicePaymentRefund.getReason());
        refund.setDomainRevision(invoicePaymentRefund.getDomainRevision());

        if (invoicePaymentRefund.isSetCash()) {
            Cash refundCash = invoicePaymentRefund.getCash();
            refund.setAmount(refundCash.getAmount());
            refund.setCurrencyCode(refundCash.getCurrency().getSymbolicCode());
        } else {
            refund.setAmount(payment.getAmount());
            refund.setCurrencyCode(payment.getCurrencyCode());
        }

        Map<CashFlowType, Long> cashFlow = DamselUtil.parseCashFlow(invoicePaymentRefundCreated.getCashFlow());
        refund.setFee(cashFlow.getOrDefault(FEE, 0L) - cashFlow.getOrDefault(RETURN_FEE, 0L));

        refundDao.save(refund);
        log.info("Refund have been saved, refund={}", refund);
    }

}
