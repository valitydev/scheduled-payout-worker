package dev.vality.scheduledpayoutworker.dao;

import dev.vality.scheduledpayoutworker.exception.DaoException;

public interface PayoutDao extends GenericDao {

    long getAvailableAmount(String payoutId) throws DaoException;
}
