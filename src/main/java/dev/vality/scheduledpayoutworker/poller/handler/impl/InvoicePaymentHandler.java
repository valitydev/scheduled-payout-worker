package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.FinalCashFlowPosting;
import dev.vality.damsel.domain.InvoicePayment;
import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentStarted;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.enums.PaymentStatus;
import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import dev.vality.scheduledpayoutworker.util.CashFlowType;
import dev.vality.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static dev.vality.scheduledpayoutworker.util.CashFlowType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentHandler implements PaymentProcessingHandler {

    private final InvoiceDao invoiceDao;

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStarted()
                && invoiceDao.get(event.getSourceId()) != null;
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
            throw new NotFoundException(
                    String.format("Invoice on payment not found, invoiceId='%s', paymentId='%s'",
                            invoiceId, invoicePayment.getId()));
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
