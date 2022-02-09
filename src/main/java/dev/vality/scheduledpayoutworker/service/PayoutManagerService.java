package dev.vality.scheduledpayoutworker.service;

import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.exception.StorageException;
import org.apache.thrift.TException;

import java.time.LocalDateTime;

public interface PayoutManagerService {

    /**
     * Create payout by partyId, shopId and toTime. fromTime = toTime - MAX_DAYS
     * @return payoutId if payout successfully created, null if payout amount = 0
     */
    String createPayoutByRange(
            String partyId,
            String shopId,
            LocalDateTime toTime
    ) throws NotFoundException, StorageException, TException;

}
