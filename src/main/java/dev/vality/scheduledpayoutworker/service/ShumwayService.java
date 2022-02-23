package dev.vality.scheduledpayoutworker.service;

import java.time.LocalDateTime;

public interface ShumwayService {

    long getAccountBalance(long accountId, LocalDateTime fromTime, LocalDateTime toTime);
}
