package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import org.apache.thrift.TException;

public interface SchedulatorService {

    void registerJob(String partyId, String shopId, BusinessScheduleRef schedule);

    void deregisterJob(String partyId, String shopId);
}
