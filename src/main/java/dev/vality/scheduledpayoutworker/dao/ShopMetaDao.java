package dev.vality.scheduledpayoutworker.dao;

import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.exception.DaoException;

import java.time.LocalDateTime;


public interface ShopMetaDao extends GenericDao {

    void update(String partyId, String shopId) throws DaoException;

    void update(String partyId, String shopId, LocalDateTime payoutCreatedAt) throws DaoException;

    void update(String partyId,
                String shopId,
                int calendarId,
                int schedulerId,
                String payoutScheduleId) throws DaoException;

    ShopMeta get(String partyId, String shopId) throws DaoException;

    void disableShop(String partyId, String shopId) throws DaoException;
}
