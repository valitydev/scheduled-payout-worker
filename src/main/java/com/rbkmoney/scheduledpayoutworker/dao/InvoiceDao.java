package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface InvoiceDao extends GenericDao {

    void save(String invoiceId, String partyId, String shopId, String contractId,
              Long partyRevision, LocalDateTime createdAt) throws DaoException;

    Invoice get(String invoiceId) throws DaoException;

}
