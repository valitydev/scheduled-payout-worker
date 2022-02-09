package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Invoice;
import dev.vality.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface InvoiceDao extends GenericDao {

    void save(String invoiceId, String partyId, String shopId,
              Long partyRevision, LocalDateTime createdAt) throws DaoException;

    Invoice get(String invoiceId) throws DaoException;

}
