package dev.vality.scheduledpayoutworker.service;

import java.time.LocalDateTime;

public interface ShumwayService {

    long getAccountBalanceDiff(long accountId, LocalDateTime fromTime, LocalDateTime toTime);

    long getAccountBalance(long accountId, LocalDateTime dateTime);
}
