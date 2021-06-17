package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.scheduledpayoutworker.integration.AbstractPosgresIntegrationTest;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentDaoTest extends AbstractPosgresIntegrationTest {

    @Autowired
    PaymentDao paymentDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Payment payment = random(Payment.class, "payoutId");
        paymentDao.save(payment);

        Payment secondPayment = new Payment(payment);
        secondPayment.setId(null);
        paymentDao.save(secondPayment);

        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Payment payment = random(Payment.class, "payoutId", "externalFee",
                "capturedAt", "terminalId", "domainRevision");
        paymentDao.save(payment);
        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

}
