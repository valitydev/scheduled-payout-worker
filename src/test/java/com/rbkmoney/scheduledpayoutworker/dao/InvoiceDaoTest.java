package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.scheduledpayoutworker.config.AbstractPostgreTestContainerConfig;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {"kafka.topics.invoice.enabled=false",
        "kafka.topics.party-management.enabled=false"})
class InvoiceDaoTest extends AbstractPostgreTestContainerConfig {

    @Autowired
    InvoiceDao invoiceDao;

    @Test
    void testSaveAndGet() throws DaoException {
        Invoice invoice = random(Invoice.class);

        invoiceDao.save(invoice.getId(), invoice.getPartyId(), invoice.getShopId(),
                null, invoice.getCreatedAt());
        //save again
        invoiceDao.save(invoice.getId(), invoice.getPartyId(), invoice.getShopId(),
                invoice.getPartyRevision(), invoice.getCreatedAt());

        assertEquals(invoice, invoiceDao.get(invoice.getId()));
    }

}
