package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;

import java.time.LocalDateTime;

public interface PayoutManagerService {

    String createPayoutByRange(
            String partyId,
            String shopId,
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) throws NotFoundException, StorageException;

}
