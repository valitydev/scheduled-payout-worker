package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.InvoicePaymentChargeback;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChargebackChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChargebackCreated;
import dev.vality.geck.common.util.TBaseUtil;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.enums.ChargebackStage;
import dev.vality.payouter.domain.enums.ChargebackStatus;
import dev.vality.payouter.domain.tables.pojos.Chargeback;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.dao.ChargebackDao;
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
public class InvoicePaymentChargebackHandler implements PaymentProcessingHandler {

    private final ChargebackDao chargebackDao;

    private final PaymentDao paymentDao;

    private final InvoiceDao invoiceDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .isSetInvoicePaymentChargebackChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentChargebackChange().getPayload()
                .isSetInvoicePaymentChargebackCreated()
                && invoiceDao.get(event.getSourceId()) != null;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();
        InvoicePaymentChargebackCreated invoicePaymentChargebackCreated = invoicePaymentChargebackChange.getPayload()
                .getInvoicePaymentChargebackCreated();

        InvoicePaymentChargeback invoicePaymentChargeback = invoicePaymentChargebackCreated.getChargeback();

        Payment payment = paymentDao.get(invoiceId, paymentId);

        if (payment == null) {
            throw new NotFoundException(
                    String.format("Payment on chargeback not found, invoiceId='%s', paymentId='%s', chargebackId='%s'",
                            invoiceId, paymentId, invoicePaymentChargeback.getId()));
        }

        Chargeback chargeback = new Chargeback();
        chargeback.setEventId(eventId);
        chargeback.setPartyId(payment.getPartyId());
        chargeback.setShopId(payment.getShopId());
        chargeback.setInvoiceId(invoiceId);
        chargeback.setPaymentId(paymentId);
        chargeback.setChargebackId(invoicePaymentChargeback.getId());
        chargeback.setStatus(ChargebackStatus.PENDING);
        chargeback.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentChargeback.getCreatedAt()));
        if (invoicePaymentChargeback.isSetBody()) {
            Cash chargebackCash = invoicePaymentChargeback.getBody();
            chargeback.setAmount(chargebackCash.getAmount());
            chargeback.setCurrencyCode(chargebackCash.getCurrency().getSymbolicCode());
        } else {
            chargeback.setAmount(payment.getAmount());
            chargeback.setCurrencyCode(payment.getCurrencyCode());
        }
        chargeback.setLevyAmount(invoicePaymentChargeback.getLevy().getAmount());
        chargeback.setLevyCurrencyCode(invoicePaymentChargeback.getLevy().getCurrency().getSymbolicCode());
        chargeback.setChargebackStage(
                TBaseUtil.unionFieldToEnum(invoicePaymentChargeback.getStage(), ChargebackStage.class)
        );

        chargebackDao.save(chargeback);
        log.info("Chargeback have been saved, chargeback={}", chargeback);
    }

}
