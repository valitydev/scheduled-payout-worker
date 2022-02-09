package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.BusinessScheduleRef;

public interface SchedulatorService {

    void registerJob(String partyId, String shopId, BusinessScheduleRef schedule);

    void deregisterJob(String partyId, String shopId);
}
