package dev.vality.scheduledpayoutworker.dao.impl;

import dev.vality.payouter.domain.enums.AdjustmentStatus;
import dev.vality.payouter.domain.tables.pojos.Adjustment;
import dev.vality.payouter.domain.tables.records.AdjustmentRecord;
import dev.vality.scheduledpayoutworker.dao.AdjustmentDao;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static dev.vality.payouter.domain.Tables.ADJUSTMENT;

@Component
public class AdjustmentDaoImpl extends AbstractGenericDao implements AdjustmentDao {

    private final RowMapper<Adjustment> adjustmentRowMapper;

    @Autowired
    public AdjustmentDaoImpl(DataSource dataSource) {
        super(dataSource);
        adjustmentRowMapper = new RecordRowMapper<>(ADJUSTMENT, Adjustment.class);
    }

    @Override
    public void save(Adjustment adjustment) throws DaoException {
        AdjustmentRecord adjustmentRecord = getDslContext().newRecord(ADJUSTMENT, adjustment);
        adjustmentRecord.reset(ADJUSTMENT.PAYOUT_ID);
        Query query = getDslContext().insertInto(ADJUSTMENT)
                .set(adjustmentRecord)
                .onConflict(ADJUSTMENT.INVOICE_ID, ADJUSTMENT.PAYMENT_ID, ADJUSTMENT.ADJUSTMENT_ID)
                .doUpdate()
                .set(adjustmentRecord);
        executeOne(query);
    }

    @Override
    public Adjustment get(String invoiceId, String paymentId, String adjustmentId) throws DaoException {
        Query query = getDslContext().selectFrom(ADJUSTMENT)
                .where(ADJUSTMENT.INVOICE_ID.eq(invoiceId)
                        .and(ADJUSTMENT.PAYMENT_ID.eq(paymentId))
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)));

        return fetchOne(query, adjustmentRowMapper);
    }

    @Override
    public void markAsCaptured(long eventId, String invoiceId, String paymentId, String adjustmentId,
                               LocalDateTime capturedAt) throws DaoException {
        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.STATUS, AdjustmentStatus.CAPTURED)
                .set(ADJUSTMENT.CAPTURED_AT, capturedAt)
                .where(ADJUSTMENT.PAYMENT_ID.eq(paymentId)
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)
                                .and(ADJUSTMENT.INVOICE_ID.eq(invoiceId))));
        executeOne(query);
    }

    @Override
    public void markAsCancelled(long eventId, String invoiceId, String paymentId, String adjustmentId)
            throws DaoException {
        Query query = getDslContext().update(ADJUSTMENT)
                .set(ADJUSTMENT.STATUS, AdjustmentStatus.CANCELLED)
                .where(ADJUSTMENT.PAYMENT_ID.eq(paymentId)
                        .and(ADJUSTMENT.ADJUSTMENT_ID.eq(adjustmentId)
                                .and(ADJUSTMENT.INVOICE_ID.eq(invoiceId))));
        executeOne(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException {
        Query query = getDslContext()
                .update(ADJUSTMENT)
                .set(ADJUSTMENT.PAYOUT_ID, payoutId)
                .where(
                        ADJUSTMENT.PARTY_ID.eq(partyId)
                                .and(ADJUSTMENT.SHOP_ID.eq(shopId))
                                .and(ADJUSTMENT.CAPTURED_AT.between(from, to))
                                .and(ADJUSTMENT.STATUS.eq(AdjustmentStatus.CAPTURED))
                                .and(ADJUSTMENT.PAYOUT_ID.isNull())
                );

        return execute(query);
    }

}
