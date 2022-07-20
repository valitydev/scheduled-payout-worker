package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.domain.BusinessScheduleRef;
import dev.vality.damsel.domain.CalendarRef;
import dev.vality.damsel.domain.PaymentInstitution;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.schedule.DominantBasedSchedule;
import dev.vality.damsel.schedule.RegisterJobRequest;
import dev.vality.damsel.schedule.SchedulatorSrv;
import dev.vality.damsel.schedule.Schedule;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.model.ScheduledJobContext;
import dev.vality.scheduledpayoutworker.serde.impl.ScheduledJobSerializer;
import dev.vality.scheduledpayoutworker.service.DominantService;
import dev.vality.scheduledpayoutworker.service.PartyManagementService;
import dev.vality.scheduledpayoutworker.service.SchedulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.vality.scheduledpayoutworker.util.GenerateUtil.generatePayoutScheduleId;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulatorServiceImpl implements SchedulatorService {

    private final DominantService dominantService;
    private final PartyManagementService partyManagementService;

    private final ShopMetaDao shopMetaDao;

    private final SchedulatorSrv.Iface schedulatorClient;

    private final ScheduledJobSerializer scheduledJobSerializer;

    @Value("${service.schedulator.callback-path}")
    private String callbackPath;

    @Override
    @Transactional
    public void registerJob(String partyId, String shopId, BusinessScheduleRef scheduleRef) {
        log.info("Trying to send create job request, partyId='{}', shopId='{}', scheduleRef='{}'",
                partyId, shopId, scheduleRef);

        Shop shop = partyManagementService.getShop(partyId, shopId);
        var paymentInstitutionRef = partyManagementService.getPaymentInstitutionRef(partyId, shop.getContractId());
        PaymentInstitution paymentInstitution = dominantService.getPaymentInstitution(paymentInstitutionRef);
        if (!paymentInstitution.isSetCalendar()) {
            throw new NotFoundException(String.format("Calendar not found, " +
                    "partyId='%s', shopId='%s', contractId='%s'", partyId, shop.getId(), shop.getContractId()));
        }

        try {
            deregisterJob(partyId, shopId);
        } catch (IllegalStateException e) {
            log.info("Job not found, so we couldn't deregister it");
        }

        CalendarRef calendarRef = paymentInstitution.getCalendar();
        Schedule schedule = new Schedule();
        DominantBasedSchedule dominantBasedSchedule = new DominantBasedSchedule()
                .setBusinessScheduleRef(new BusinessScheduleRef().setId(scheduleRef.getId()))
                .setCalendarRef(calendarRef);
        schedule.setDominantSchedule(dominantBasedSchedule);

        String payoutScheduleId = generatePayoutScheduleId(partyId, shopId, scheduleRef.getId());
        ScheduledJobContext context = new ScheduledJobContext();
        context.setPartyId(partyId);
        context.setShopId(shopId);
        context.setJobId(payoutScheduleId);

        RegisterJobRequest registerJobRequest = new RegisterJobRequest()
                .setSchedule(schedule)
                .setExecutorServicePath(callbackPath)
                .setContext(scheduledJobSerializer.writeByte(context));

        try {
            schedulatorClient.registerJob(payoutScheduleId, registerJobRequest);
        } catch (TException e) {
            throw new IllegalStateException(String.format("Register job '%s' failed", payoutScheduleId), e);
        }

        shopMetaDao.update(partyId, shopId, calendarRef.getId(), scheduleRef.getId(), payoutScheduleId);

        log.info("Create job request have been successfully sent, " +
                        "partyId='{}', shopId='{}', calendarRef='{}', scheduleRef='{}'",
                partyId, shopId, calendarRef, scheduleRef);
    }

    @Override
    @Transactional
    public void deregisterJob(String partyId, String shopId) {
        ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
        log.info("Trying to deregister job, partyId='{}', shopId='{}'", partyId, shopId);
        shopMetaDao.disableShop(partyId, shopId);
        if (shopMeta.getPayoutScheduleId() != null) {
            try {
                schedulatorClient.deregisterJob(shopMeta.getPayoutScheduleId());
            } catch (TException e) {
                throw new IllegalStateException(
                        String.format("Deregister job '%s' failed", shopMeta.getSchedulerId()), e);
            }
        }
    }
}
