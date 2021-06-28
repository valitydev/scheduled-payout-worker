package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.scheduledpayoutworker.config.AbstractPostgreTestContainerConfig;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
