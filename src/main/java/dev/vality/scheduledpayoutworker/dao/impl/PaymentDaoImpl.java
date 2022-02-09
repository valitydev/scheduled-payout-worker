package dev.vality.scheduledpayoutworker.dao.impl;

import dev.vality.payouter.domain.enums.PaymentStatus;
import dev.vality.payouter.domain.tables.pojos.Payment;
import dev.vality.payouter.domain.tables.records.PaymentRecord;
import dev.vality.scheduledpayoutworker.dao.PaymentDao;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static dev.vality.payouter.domain.Tables.PAYMENT;

@Component
public class PaymentDaoImpl extends AbstractGenericDao implements PaymentDao {

    private final RowMapper<Payment> paymentRowMapper;

    @Autowired
    public PaymentDaoImpl(DataSource dataSource) {
        super(dataSource);
        paymentRowMapper = new RecordRowMapper<>(PAYMENT, Payment.class);
    }

    @Override
    public void save(Payment payment) throws DaoException {
        PaymentRecord paymentRecord = getDslContext().newRecord(PAYMENT, payment);
        paymentRecord.reset(PAYMENT.PAYOUT_ID);
        Query query = getDslContext()
                .insertInto(PAYMENT)
                .set(paymentRecord)
                .onConflict(PAYMENT.INVOICE_ID, PAYMENT.PAYMENT_ID)
                .doUpdate()
                .set(paymentRecord);
        executeOne(query);
    }

    @Override
    public Payment get(String invoiceId, String paymentId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        return fetchOne(query, paymentRowMapper);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.PAYOUT_ID, payoutId)
                .where(PAYMENT.STATUS.eq(PaymentStatus.CAPTURED)
                        .and(PAYMENT.PARTY_ID.eq(partyId))
                        .and(PAYMENT.SHOP_ID.eq(shopId))
                        .and(PAYMENT.CAPTURED_AT.between(from, to))
                        .and(PAYMENT.PAYOUT_ID.isNull()));
        return execute(query);
    }

    @Override
    public void markAsCaptured(Long eventId, String invoiceId, String paymentId, LocalDateTime capturedAt)
            throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.EVENT_ID, eventId)
                .set(PAYMENT.STATUS, PaymentStatus.CAPTURED)
                .set(PAYMENT.CAPTURED_AT, capturedAt)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.PAYMENT_ID.eq(paymentId)));

        execute(query);
    }

    @Override
    public void markAsCancelled(Long eventId, String invoiceId, String paymentId) throws DaoException {
        Query query = getDslContext().update(PAYMENT)
                .set(PAYMENT.EVENT_ID, eventId)
                .set(PAYMENT.STATUS, PaymentStatus.CANCELLED)
                .where(
                        PAYMENT.INVOICE_ID.eq(invoiceId)
                                .and(PAYMENT.PAYMENT_ID.eq(paymentId))
                                .and(PAYMENT.PAYOUT_ID.isNull())
                );

        execute(query);
    }

}
