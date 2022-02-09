package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;

public interface PaymentDao extends GenericDao {

    void save(Payment payment) throws DaoException;

    Payment get(String invoiceId, String paymentId) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException;

    void markAsCaptured(Long eventId, String invoiceId, String paymentId, LocalDateTime capturedAt) throws DaoException;

    void markAsCancelled(Long eventId, String invoiceId, String paymentId) throws DaoException;
}
