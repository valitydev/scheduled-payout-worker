package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface ChargebackDao extends GenericDao {

    void save(Chargeback chargeback) throws DaoException;

    Chargeback get(String invoiceId, String paymentId, String chargebackId) throws DaoException;

    void markAsAccepted(long eventId, String invoiceId, String paymentId, String chargebackId,
                        LocalDateTime succeededAt) throws DaoException;

    void markAsRejected(long eventId, String invoiceId, String paymentId, String chargebackId) throws DaoException;

    void markAsCancelled(long eventId, String invoiceId, String paymentId, String chargebackId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException;

    int excludeFromPayout(String payoutId) throws DaoException;

}
