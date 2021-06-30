package com.rbkmoney.scheduledpayoutworker.dao.impl;

import com.rbkmoney.scheduledpayoutworker.dao.PayoutDao;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static com.rbkmoney.payouter.domain.Tables.*;
import static com.rbkmoney.payouter.domain.tables.Refund.REFUND;

@Component
public class PayoutDaoImpl extends AbstractGenericDao implements PayoutDao {

    @Autowired
    public PayoutDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getAvailableAmount(String payoutId) throws DaoException {
        Field<Long> paymentAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(PAYMENT.AMOUNT.minus(PAYMENT.FEE).minus(PAYMENT.GUARANTEE_DEPOSIT)), 0L))
                .from(PAYMENT).where(PAYMENT.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> refundAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(REFUND.AMOUNT.plus(REFUND.FEE)), 0L))
                .from(REFUND).where(REFUND.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> chargebackAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(CHARGEBACK.AMOUNT), 0L))
                .from(CHARGEBACK).where(CHARGEBACK.PAYOUT_ID.eq(payoutId)).asField();

        Field<Long> adjustmentAmount = getDslContext()
                .select(DSL.coalesce(DSL.sum(ADJUSTMENT.AMOUNT), 0L))
                .from(ADJUSTMENT).where(ADJUSTMENT.PAYOUT_ID.eq(payoutId)).asField();

        Query query = getDslContext().select(
                paymentAmount
                        .plus(adjustmentAmount)
                        .minus(chargebackAmount)
                        .minus(refundAmount)
        );

        return fetchOne(query, Long.class);
    }

}
