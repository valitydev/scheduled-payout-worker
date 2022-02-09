package dev.vality.scheduledpayoutworker.dao.impl;

import dev.vality.payouter.domain.enums.ChargebackStatus;
import dev.vality.payouter.domain.tables.pojos.Chargeback;
import dev.vality.payouter.domain.tables.records.ChargebackRecord;
import dev.vality.scheduledpayoutworker.dao.ChargebackDao;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.jooq.Query;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static dev.vality.payouter.domain.tables.Chargeback.CHARGEBACK;

@Component
public class ChargebackDaoImpl extends AbstractGenericDao implements ChargebackDao {

    private final RowMapper<Chargeback> chargebackRowMapper;

    public ChargebackDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.chargebackRowMapper = new RecordRowMapper<>(CHARGEBACK, Chargeback.class);
    }


    @Override
    public void save(Chargeback chargeback) throws DaoException {
        ChargebackRecord chargebackRecord = getDslContext().newRecord(CHARGEBACK, chargeback);
        chargebackRecord.reset(CHARGEBACK.PAYOUT_ID);
        Query query = getDslContext().insertInto(CHARGEBACK)
                .set(chargebackRecord)
                .onConflict(CHARGEBACK.INVOICE_ID, CHARGEBACK.PAYMENT_ID, CHARGEBACK.CHARGEBACK_ID)
                .doUpdate()
                .set(chargebackRecord);
        executeOne(query);
    }

    @Override
    public Chargeback get(String invoiceId, String paymentId, String chargebackId) throws DaoException {
        Query query = getDslContext().selectFrom(CHARGEBACK)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId)
                        .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                        .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId)));

        return fetchOne(query, chargebackRowMapper);
    }

    @Override
    public void markAsAccepted(long eventId, String invoiceId, String paymentId, String chargebackId,
                               LocalDateTime succeededAt) throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.SUCCEEDED)
                .set(CHARGEBACK.SUCCEEDED_AT, succeededAt)
                .where(CHARGEBACK.INVOICE_ID.eq(invoiceId)
                        .and(CHARGEBACK.PAYMENT_ID.eq(paymentId)
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))));
        executeOne(query);
    }

    @Override
    public void markAsRejected(long eventId, String invoiceId, String paymentId, String chargebackId)
            throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.REJECTED)
                .where(
                        CHARGEBACK.INVOICE_ID.eq(invoiceId)
                                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public void markAsCancelled(long eventId, String invoiceId, String paymentId, String chargebackId)
            throws DaoException {
        Query query = getDslContext().update(CHARGEBACK)
                .set(CHARGEBACK.STATUS, ChargebackStatus.CANCELLED)
                .where(
                        CHARGEBACK.INVOICE_ID.eq(invoiceId)
                                .and(CHARGEBACK.PAYMENT_ID.eq(paymentId))
                                .and(CHARGEBACK.CHARGEBACK_ID.eq(chargebackId))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                );
        executeOne(query);
    }

    @Override
    public int includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime from, LocalDateTime to)
            throws DaoException {
        Query query = getDslContext()
                .update(CHARGEBACK)
                .set(CHARGEBACK.PAYOUT_ID, payoutId)
                .where(
                        CHARGEBACK.PARTY_ID.eq(partyId)
                                .and(CHARGEBACK.SHOP_ID.eq(shopId))
                                .and(CHARGEBACK.SUCCEEDED_AT.between(from, to))
                                .and(CHARGEBACK.PAYOUT_ID.isNull())
                                .and(CHARGEBACK.STATUS.eq(ChargebackStatus.SUCCEEDED))
                );
        return execute(query);
    }

}
