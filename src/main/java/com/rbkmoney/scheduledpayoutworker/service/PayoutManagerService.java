package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;
import org.apache.thrift.TException;

import java.time.LocalDateTime;

public interface PayoutManagerService {

    String createPayoutByRange(
            String partyId,
            String shopId,
            LocalDateTime toTime
    ) throws NotFoundException, StorageException, TException;

}
