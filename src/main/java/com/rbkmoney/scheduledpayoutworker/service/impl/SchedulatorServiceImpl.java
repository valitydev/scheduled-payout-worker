package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.domain.CalendarRef;
import com.rbkmoney.damsel.domain.PaymentInstitution;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.schedule.DominantBasedSchedule;
import com.rbkmoney.damsel.schedule.RegisterJobRequest;
import com.rbkmoney.damsel.schedule.SchedulatorSrv;
import com.rbkmoney.damsel.schedule.Schedule;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.service.DominantService;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementService;
import com.rbkmoney.scheduledpayoutworker.service.SchedulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulatorServiceImpl implements SchedulatorService {

    private final DominantService dominantService;
    private final PartyManagementService partyManagementService;

    private final ShopMetaDao shopMetaDao;

    private final SchedulatorSrv.Iface schedulatorClient;

    @Override
    public void registerJob(String partyId, String shopId, BusinessScheduleRef scheduleRef) {
        //TODO: Проверить обязательные поля thrift'а на наличие
        //log.info();

        Shop shop = partyManagementService.getShop(partyId, shopId);
        var paymentInstitutionRef = partyManagementService.getPaymentInstitutionRef(partyId, shop.getContractId());
        PaymentInstitution paymentInstitution = dominantService.getPaymentInstitution(paymentInstitutionRef);
        if (!paymentInstitution.isSetCalendar()) {
            throw new NotFoundException(String.format("Calendar not found, " +
                    "partyId='%s', shopId='%s', contractId='%s'", partyId, shop.getId(), shop.getContractId()));
        }

        CalendarRef calendarRef = paymentInstitution.getCalendar();
        long revision = partyManagementService.getPartyRevision(partyId);
        shopMetaDao.save(partyId, shopId, calendarRef.getId(), scheduleRef.getId());

        Schedule schedule = new Schedule();
        DominantBasedSchedule dominantBasedSchedule = new DominantBasedSchedule()
                .setBusinessScheduleRef(new BusinessScheduleRef().setId(scheduleRef.getId()))
                .setCalendarRef(calendarRef)
                .setRevision(revision);
        schedule.setDominantSchedule(dominantBasedSchedule);
        RegisterJobRequest registerJobRequest = new RegisterJobRequest()
                .setSchedule(schedule)
                //TODO: Подсмотрел в clickhouse-notificator такое заполнение, уточнить в необходимости.
                .setContext(new byte[0]);
        //TODO: Откуда брать scheduleId
        try {
            schedulatorClient.registerJob(String.valueOf(scheduleRef.getId()), registerJobRequest);
        } catch (TException e) {
            //TODO: Обработка ScheduleAlreadyExists (вероятно на уровне вставки в shopMetaDao)
            throw new IllegalStateException(String.format("Register job '%s' failed", scheduleRef.getId()), e);
        }
    }

    @Override
    public void deregisterJob(String partyId, String shopId) {
        ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
        if (shopMeta != null) {
            log.info("Trying to deregister job, partyId='{}', shopId='{}'", partyId, shopId);
            shopMetaDao.disableShop(partyId, shopId);
            if (shopMeta.getSchedulerId() != null) {
                try {
                    schedulatorClient.deregisterJob(String.valueOf(shopMeta.getSchedulerId()));
                } catch (TException e) {
                    //TODO: Обработка ScheduleAlreadyExists (вероятно на уровне вставки в shopMetaDao)
                    throw new IllegalStateException(
                            String.format("Deregister job '%s' failed", shopMeta.getSchedulerId()), e);
                }
            }
        }
    }

}
