package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Adjustment;
import dev.vality.scheduledpayoutworker.config.AbstractPostgreTestContainerConfig;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {"kafka.topics.invoice.enabled=false",
        "kafka.topics.party-management.enabled=false"})
class AdjustmentDaoTest extends AbstractPostgreTestContainerConfig {

    @Autowired
    AdjustmentDao adjustmentDao;

    @Test
    void testSaveAndGet() throws DaoException {
        Adjustment adjustment = random(Adjustment.class, "payoutId");
        adjustmentDao.save(adjustment);
        Adjustment secondAdjustment = new Adjustment(adjustment);
        adjustmentDao.save(secondAdjustment);

        assertEquals(
                adjustment,
                adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

    @Test
    void testSaveOnlyNonNullValues() throws DaoException {
        Adjustment adjustment = random(Adjustment.class, "payoutId");
        adjustmentDao.save(adjustment);
        assertEquals(
                adjustment,
                adjustmentDao.get(adjustment.getInvoiceId(), adjustment.getPaymentId(), adjustment.getAdjustmentId()));
    }

}
