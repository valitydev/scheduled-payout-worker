package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Chargeback;
import dev.vality.scheduledpayoutworker.config.AbstractPostgreTestContainerConfig;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {"kafka.topics.invoice.enabled=false",
        "kafka.topics.party-management.enabled=false"})
class ChargebackDaoTest extends AbstractPostgreTestContainerConfig {

    @Autowired
    ChargebackDao chargebackDao;

    @Test
    void testSaveAndGet() throws DaoException {
        Chargeback chargeback = random(Chargeback.class, "payoutId");
        chargebackDao.save(chargeback);

        Chargeback secChargeback = new Chargeback(chargeback);
        secChargeback.setId(null);
        chargebackDao.save(secChargeback);

        assertEquals(
                chargeback,
                chargebackDao.get(chargeback.getInvoiceId(), chargeback.getPaymentId(), chargeback.getChargebackId()));
    }

    @Test
    void testSaveOnlyNonNullValues() throws DaoException {
        Chargeback chargeback = random(Chargeback.class, "reason", "payoutId");
        chargebackDao.save(chargeback);
        assertEquals(
                chargeback,
                chargebackDao.get(chargeback.getInvoiceId(), chargeback.getPaymentId(), chargeback.getChargebackId()));
    }

}
