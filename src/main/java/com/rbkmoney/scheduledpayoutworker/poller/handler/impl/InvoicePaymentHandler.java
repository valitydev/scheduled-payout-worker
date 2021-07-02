package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.domain.InvoicePayment;
import com.rbkmoney.damsel.domain.PaymentRoute;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentStarted;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.InvoiceDao;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.scheduledpayoutworker.util.CashFlowType;
import com.rbkmoney.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.rbkmoney.scheduledpayoutworker.util.CashFlowType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentHandler implements PaymentProcessingHandler {

    private final InvoiceDao invoiceDao;

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStarted();
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        InvoicePaymentStarted invoicePaymentStarted = invoiceChange
                .getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentStarted();

        Payment payment = new Payment();
        InvoicePayment invoicePayment = invoicePaymentStarted.getPayment();

        payment.setEventId(event.getEventId());
        String invoiceId = event.getSourceId();
        payment.setInvoiceId(invoiceId);

        Invoice invoice = invoiceDao.get(invoiceId);
        if (invoice == null) {
            log.debug("Invoice on payment not found, invoiceId='{}', paymentId='{}'",
                    invoiceId, invoicePayment.getId());
            return;
        }

        payment.setPartyId(invoice.getPartyId());
        payment.setShopId(invoice.getShopId());

        payment.setPaymentId(invoicePayment.getId());
        payment.setStatus(PaymentStatus.PENDING);

        Cash cash = invoicePayment.getCost();
        payment.setAmount(cash.getAmount());
        payment.setCurrencyCode(cash.getCurrency().getSymbolicCode());

        Instant paymentCreatedAt = TypeUtil.stringToInstant(invoicePayment.getCreatedAt());
        payment.setCreatedAt(LocalDateTime.ofInstant(paymentCreatedAt, ZoneOffset.UTC));
        payment.setDomainRevision(invoicePayment.getDomainRevision());

        if (invoicePayment.isSetPartyRevision()) {
            payment.setPartyRevision(invoicePayment.getPartyRevision());
        }

        if (invoicePaymentStarted.isSetRoute()) {
            PaymentRoute paymentRoute = invoicePaymentStarted.getRoute();
            int providerId = paymentRoute.getProvider().getId();
            payment.setProviderId(providerId);
            int terminalId = paymentRoute.getTerminal().getId();
            payment.setTerminalId(terminalId);
        }

        if (invoicePaymentStarted.isSetCashFlow()) {
            List<FinalCashFlowPosting> finalCashFlow = invoicePaymentStarted.getCashFlow();
            Map<CashFlowType, Long> parsedCashFlow = DamselUtil.parseCashFlow(finalCashFlow);
            payment.setFee(parsedCashFlow.getOrDefault(FEE, 0L));
            payment.setProviderFee(parsedCashFlow.getOrDefault(PROVIDER_FEE, 0L));
            payment.setExternalFee(parsedCashFlow.getOrDefault(EXTERNAL_FEE, 0L));
            payment.setGuaranteeDeposit(parsedCashFlow.getOrDefault(GUARANTEE_DEPOSIT, 0L));
        }

        paymentDao.save(payment);
        log.info("Payment have been saved, payment={}", payment);
    }

}
