package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.config.AbstractPostgreTestContainerConfig;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {"kafka.topics.invoice.enabled=false",
        "kafka.topics.party-management.enabled=false"})
class PaymentDaoTest extends AbstractPostgreTestContainerConfig {

    @Autowired
    PaymentDao paymentDao;

    @Test
    void testSaveAndGet() throws DaoException {
        Payment payment = random(Payment.class, "payoutId");
        paymentDao.save(payment);

        Payment secondPayment = new Payment(payment);
        secondPayment.setId(null);
        paymentDao.save(secondPayment);

        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

    @Test
    void testSaveOnlyNonNullValues() throws DaoException {
        Payment payment = random(Payment.class, "payoutId", "externalFee",
                "capturedAt", "terminalId", "domainRevision");
        paymentDao.save(payment);
        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

}
