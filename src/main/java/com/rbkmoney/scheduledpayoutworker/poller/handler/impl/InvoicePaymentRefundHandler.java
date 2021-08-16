package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefundChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefundCreated;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.enums.RefundStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.dao.RefundDao;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.scheduledpayoutworker.util.CashFlowType;
import com.rbkmoney.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.rbkmoney.scheduledpayoutworker.util.CashFlowType.FEE;
import static com.rbkmoney.scheduledpayoutworker.util.CashFlowType.RETURN_FEE;

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
