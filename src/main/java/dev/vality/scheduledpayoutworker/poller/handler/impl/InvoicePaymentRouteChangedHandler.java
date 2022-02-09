package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.PaymentRoute;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
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
public class InvoicePaymentRouteChangedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRouteChanged()
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
                    String.format("Invoice on payment not found, invoiceId='%s', paymentId='%s'", invoiceId,
                            paymentId));
        }

        PaymentRoute paymentRoute = invoicePaymentChange.getPayload().getInvoicePaymentRouteChanged().getRoute();
        payment.setProviderId(paymentRoute.getProvider().getId());
        payment.setTerminalId(paymentRoute.getTerminal().getId());

        paymentDao.save(payment);
        log.info("Payment route have been saved, route='{}', invoiceId='{}', paymentId='{}'",
                paymentRoute, invoiceId, paymentId);
    }

}
