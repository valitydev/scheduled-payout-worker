package dev.vality.scheduledpayoutworker.dao.impl;

import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static dev.vality.payouter.domain.Tables.SHOP_META;

@Component
public class ShopMetaDaoImpl extends AbstractGenericDao implements ShopMetaDao {

    private final RowMapper<ShopMeta> shopMetaRowMapper;

    @Autowired
    public ShopMetaDaoImpl(DataSource dataSource) {
        super(dataSource);
        shopMetaRowMapper = new RecordRowMapper<>(SHOP_META, ShopMeta.class);
    }

    @Override
    public void update(String partyId, String shopId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(SHOP_META)
                .set(SHOP_META.PARTY_ID, partyId)
                .set(SHOP_META.SHOP_ID, shopId)
                .set(SHOP_META.WTIME, now)
                //TODO: throw exception?
                .onDuplicateKeyUpdate()
                .set(SHOP_META.WTIME, now);

        executeOne(query);
    }

    @Override
    public void update(String partyId, String shopId, LocalDateTime payoutCreatedAt) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().update(SHOP_META)
                .set(SHOP_META.LAST_PAYOUT_CREATED_AT, payoutCreatedAt)
                .set(SHOP_META.WTIME, now)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));

        executeOne(query);
    }

    @Override
    public void update(String partyId, String shopId, int calendarId, int schedulerId,
                       String payoutScheduleId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().update(SHOP_META)
                .set(SHOP_META.CALENDAR_ID, calendarId)
                .set(SHOP_META.SCHEDULER_ID, schedulerId)
                .set(SHOP_META.WTIME, now)
                .set(SHOP_META.PAYOUT_SCHEDULE_ID, payoutScheduleId)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));

        executeOne(query);
    }

    @Override
    public ShopMeta get(String partyId, String shopId) throws DaoException {
        Query query = getDslContext().selectFrom(SHOP_META)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));
        return fetchOne(query, shopMetaRowMapper);
    }

    @Override
    public void disableShop(String partyId, String shopId) throws DaoException {
        Query query = getDslContext().update(SHOP_META)
                .setNull(SHOP_META.CALENDAR_ID)
                .setNull(SHOP_META.SCHEDULER_ID)
                .setNull(SHOP_META.PAYOUT_SCHEDULE_ID)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));

        executeOne(query);
    }

}
