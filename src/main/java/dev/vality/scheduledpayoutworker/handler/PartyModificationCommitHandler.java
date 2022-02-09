package dev.vality.scheduledpayoutworker.handler;

import dev.vality.damsel.claim_management.ScheduleModification;
import dev.vality.damsel.domain.BusinessScheduleRef;
import dev.vality.scheduledpayoutworker.service.DominantService;
import dev.vality.scheduledpayoutworker.service.SchedulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyModificationCommitHandler implements CommitHandler<ScheduleModification> {

    private final SchedulatorService schedulatorService;
    private final DominantService dominantService;

    @Override
    public void accept(String partyId, String shopId, ScheduleModification scheduleModification) {
        log.info("Trying to accept payout schedule modification, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
        if (scheduleModification.isSetSchedule()) {
            BusinessScheduleRef schedule = scheduleModification.getSchedule();
            checkSchedule(schedule);
        }
        log.info("Payout schedule modification have been accepted, partyId='{}', scheduleModification='{}'",
                partyId, scheduleModification);
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

    private void checkSchedule(BusinessScheduleRef schedule) {
        if (schedule != null) {
            dominantService.getBusinessSchedule(schedule);
        }
    }

}
