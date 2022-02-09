package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.machinegun.eventsink.MachineEvent;
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

import java.util.Map;

import static dev.vality.scheduledpayoutworker.util.CashFlowType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentCashFlowChangedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange()
                .getPayload().isSetInvoicePaymentCashFlowChanged()
                && invoiceDao.get(event.getSourceId()) != null;
    }

    @Override
    public void handle(InvoiceChange change, MachineEvent event) {
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String invoiceId = event.getSourceId();
        String paymentId = invoicePaymentChange.getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(
                    String.format("Invoice payment not found, invoiceId='%s', paymentId='%s'", invoiceId, paymentId));
        }

        var finalCashFlow = invoicePaymentChange.getPayload().getInvoicePaymentCashFlowChanged().getCashFlow();
        Map<CashFlowType, Long> parsedCashFlow = DamselUtil.parseCashFlow(finalCashFlow);
        payment.setAmount(parsedCashFlow.getOrDefault(AMOUNT, 0L));
        payment.setFee(parsedCashFlow.getOrDefault(FEE, 0L));
        payment.setProviderFee(parsedCashFlow.getOrDefault(PROVIDER_FEE, 0L));
        payment.setExternalFee(parsedCashFlow.getOrDefault(EXTERNAL_FEE, 0L));
        payment.setGuaranteeDeposit(parsedCashFlow.getOrDefault(GUARANTEE_DEPOSIT, 0L));

        paymentDao.save(payment);
        log.info("Payment cash flow have been saved, finalCashFlow='{}', invoiceId='{}', paymentId='{}'",
                finalCashFlow, invoiceId, paymentId);
    }

}
