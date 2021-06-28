package com.rbkmoney.scheduledpayoutworker.handler;

import com.rbkmoney.damsel.claim_management.ScheduleModification;
import com.rbkmoney.scheduledpayoutworker.service.SchedulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyModificationCommitHandler implements CommitHandler<ScheduleModification> {

    private final SchedulatorService schedulatorService;

    @Override
    public void accept(String partyId, String shopId, ScheduleModification scheduleModification) {
    }

    @Override
    public void commit(String partyId, String shopId, ScheduleModification scheduleModification) {
        log.info("Trying to commit schedule modification, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
        if (scheduleModification.isSetSchedule()) {
            schedulatorService.registerJob(partyId, shopId, scheduleModification.getSchedule());
        } else {
            schedulatorService.deregisterJob(partyId, shopId);
        }
        log.info("Schedule modification have been committed, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
    }

}
