package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.InvoicePaymentChargeback;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackCreated;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.domain.enums.ChargebackStage;
import com.rbkmoney.payouter.domain.enums.ChargebackStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.dao.ChargebackDao;
import com.rbkmoney.scheduledpayoutworker.dao.PaymentDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentChargebackHandler implements PaymentProcessingHandler {

    private final ChargebackDao chargebackDao;

    private final PaymentDao paymentDao;

    @Override
    public boolean accept(InvoiceChange invoiceChange, MachineEvent event) {
        return invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .isSetInvoicePaymentChargebackChange()
                && invoiceChange.getInvoicePaymentChange().getPayload()
                .getInvoicePaymentChargebackChange().getPayload()
                .isSetInvoicePaymentChargebackCreated();
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
            log.debug("Payment on chargeback not found, invoiceId='{}', paymentId='{}', chargebackId='{}'",
                    invoiceId, paymentId, invoicePaymentChargeback.getId());
            return;
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
