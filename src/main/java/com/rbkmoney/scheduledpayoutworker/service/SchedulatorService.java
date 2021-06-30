package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;

public interface SchedulatorService {

    void registerJob(String partyId, String shopId, BusinessScheduleRef schedule);

    void deregisterJob(String partyId, String shopId);
}
