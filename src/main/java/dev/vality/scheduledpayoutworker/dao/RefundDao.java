package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Refund;
import dev.vality.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface RefundDao extends GenericDao {

    void save(Refund refund) throws DaoException;

    void markAsSucceeded(long eventId, String invoiceId, String paymentId, String refundId, LocalDateTime succeededAt)
            throws DaoException;

    void markAsFailed(long eventId, String invoiceId, String paymentId, String refundId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException;
}
