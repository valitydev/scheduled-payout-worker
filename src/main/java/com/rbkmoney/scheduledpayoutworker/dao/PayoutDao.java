package com.rbkmoney.scheduledpayoutworker.dao;

import com.rbkmoney.scheduledpayoutworker.exception.DaoException;

public interface PayoutDao extends GenericDao {

    long getAvailableAmount(String payoutId) throws DaoException;
}
