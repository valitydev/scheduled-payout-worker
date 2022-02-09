package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentCaptureStarted;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.dao.InvoiceDao;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentCaptureStartedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange()
                .getPayload().isSetInvoicePaymentCaptureStarted()
                && invoiceDao.get(event.getSourceId()) != null;
    }


    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(
                    String.format("Invoice payment not found, invoiceId='%s', paymentId='%s'", invoiceId, paymentId));
        }
        InvoicePaymentCaptureStarted invoicePaymentCaptureStarted = invoiceChange.getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentCaptureStarted();
        if (invoicePaymentCaptureStarted.getData().isSetCash()) {
            payment.setAmount(invoicePaymentCaptureStarted.getData().getCash().getAmount());
            payment.setCurrencyCode(invoicePaymentCaptureStarted.getData().getCash().getCurrency().getSymbolicCode());
        }
        paymentDao.save(payment);
        log.info("Payment capture started have been saved invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
    }

}
