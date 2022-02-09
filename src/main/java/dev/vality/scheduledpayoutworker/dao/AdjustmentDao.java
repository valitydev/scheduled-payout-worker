package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Adjustment;
import dev.vality.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface AdjustmentDao extends GenericDao {

    void save(Adjustment adjustment) throws DaoException;

    Adjustment get(String invoiceId, String paymentId, String adjustmentId) throws DaoException;

    void markAsCaptured(long eventId, String invoiceId, String paymentId, String adjustmentId, LocalDateTime capturedAt)
            throws DaoException;

    void markAsCancelled(long eventId, String invoiceId, String paymentId, String adjustmentId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException;
}
