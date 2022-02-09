package dev.vality.scheduledpayoutworker.dao.impl;

import dev.vality.payouter.domain.Tables;
import dev.vality.payouter.domain.enums.RefundStatus;
import dev.vality.payouter.domain.tables.pojos.Refund;
import dev.vality.payouter.domain.tables.records.RefundRecord;
import dev.vality.scheduledpayoutworker.dao.RefundDao;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static dev.vality.payouter.domain.tables.Refund.REFUND;

@Component
public class RefundDaoImpl extends AbstractGenericDao implements RefundDao {

    private final RowMapper<Refund> refundRowMapper;

    @Autowired
    public RefundDaoImpl(DataSource dataSource) {
        super(dataSource);
        refundRowMapper = new RecordRowMapper<>(REFUND, Refund.class);
    }

    @Override
    public void save(Refund refund) throws DaoException {
        RefundRecord refundRecord = getDslContext().newRecord(REFUND, refund);
        refundRecord.reset(Tables.REFUND.PAYOUT_ID);
        Query query = getDslContext().insertInto(REFUND)
                .set(refundRecord)
                .onConflict(REFUND.INVOICE_ID, REFUND.PAYMENT_ID, REFUND.REFUND_ID)
                .doUpdate()
                .set(refundRecord);
        executeOne(query);
    }

    @Override
    public void markAsSucceeded(long eventId, String invoiceId, String paymentId, String refundId,
                                LocalDateTime succeededAt) throws DaoException {
        Query query = getDslContext().update(REFUND)
                .set(REFUND.STATUS, RefundStatus.SUCCEEDED)
                .set(REFUND.SUCCEEDED_AT, succeededAt)
                .where(REFUND.INVOICE_ID.eq(invoiceId)
                        .and(REFUND.PAYMENT_ID.eq(paymentId)
                                .and(REFUND.REFUND_ID.eq(refundId))));
        executeOne(query);
    }

    @Override
    public void markAsFailed(long eventId, String invoiceId, String paymentId, String refundId) throws DaoException {
        Query query = getDslContext().update(REFUND)
                .set(REFUND.STATUS, RefundStatus.FAILED)
                .where(
                        REFUND.INVOICE_ID.eq(invoiceId)
                                .and(REFUND.PAYMENT_ID.eq(paymentId))
                                .and(REFUND.REFUND_ID.eq(refundId))
                                .and(REFUND.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId,
                             LocalDateTime from, LocalDateTime to) throws DaoException {
        Query query = getDslContext()
                .update(REFUND)
                .set(REFUND.PAYOUT_ID, payoutId)
                .where(
                        REFUND.PARTY_ID.eq(partyId)
                                .and(REFUND.SHOP_ID.eq(shopId))
                                .and(REFUND.SUCCEEDED_AT.between(from, to))
                                .and(REFUND.PAYOUT_ID.isNull())
                                .and(REFUND.STATUS.eq(RefundStatus.SUCCEEDED))
                );
        return execute(query);
    }
}
