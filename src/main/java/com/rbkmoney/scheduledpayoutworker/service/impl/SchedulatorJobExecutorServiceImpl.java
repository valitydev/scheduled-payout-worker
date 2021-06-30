package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.schedule.*;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.exception.InvalidStateException;
import com.rbkmoney.scheduledpayoutworker.exception.JobExecutionException;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;
import com.rbkmoney.scheduledpayoutworker.model.ScheduledJobContext;
import com.rbkmoney.scheduledpayoutworker.serde.impl.ScheduledJobSerializer;
import com.rbkmoney.scheduledpayoutworker.service.PayoutManagerService;
import com.rbkmoney.woody.api.flow.WFlow;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulatorJobExecutorServiceImpl implements ScheduledJobExecutorSrv.Iface {

    private final ScheduledJobSerializer scheduleJobSerializer;
    private final ShopMetaDao shopMetaDao;
    private final WFlow flow = new WFlow();
    private final PayoutManagerService payoutManagerService;

    @Override
    public ContextValidationResponse validateExecutionContext(ByteBuffer byteBuffer) throws TException {
        ScheduledJobContext scheduledJobContext = scheduleJobSerializer.read(byteBuffer.array());
        if (scheduledJobContext.getJobId() == null) {
            throw new IllegalArgumentException("Job id cannot be null!");
        }

        if (scheduledJobContext.getCalendarId() == null) {
            throw new IllegalArgumentException("Calendar id cannot be null!");
        }

        ContextValidationResponse contextValidationResponse = new ContextValidationResponse();
        ValidationResponseStatus responseStatus = new ValidationResponseStatus();
        responseStatus.setSuccess(new ValidationSuccess());
        contextValidationResponse.setResponseStatus(responseStatus);

        return contextValidationResponse;
    }

    @Override
    public ByteBuffer executeJob(ExecuteJobRequest executeJobRequest) throws TException {
        log.info("Execute job: {}", executeJobRequest);
        ScheduledJobContext scheduledJobContext =
                scheduleJobSerializer.read(executeJobRequest.getServiceExecutionContext());
        log.info("Job context: {}", scheduledJobContext);
        try {
            List<ShopMeta> shopMetas = shopMetaDao
                    .getByCalendarAndSchedulerId(scheduledJobContext.getCalendarId(), scheduledJobContext.getJobId());
            for (ShopMeta shopMeta : shopMetas) { //TODO: Check, if more then one is possible
                String partyId = shopMeta.getPartyId();
                String shopId = shopMeta.getShopId();
                log.info(
                        "Trying to create payout for shop, partyId='{}', shopId='{}', scheduledJobContext='{}'",
                        partyId, shopId, scheduledJobContext);
                try {
                    try {
                        LocalDateTime toTime = toLocalDateTime(Instant.now()); //TODO: Check correct time
                        //TODO: Check if fork really has sense
                        String payoutId = flow.createServiceFork(
                                () -> payoutManagerService.createPayoutByRange(
                                        partyId,
                                        shopId,
                                        toTime
                                )
                        ).call();

                        log.info(
                                "Payout for shop have been successfully created, payoutId='{}' partyId='{}'," +
                                        " shopId='{}', scheduledJobContext='{}'",
                                payoutId, partyId, shopId, scheduledJobContext);
                    } catch (NotFoundException | InvalidStateException ex) {
                        log.warn(
                                "Failed to generate payout, partyId='{}', shopId='{}', scheduledJobContext='{}'," +
                                        " reason='{}'",
                                partyId, shopId, scheduledJobContext, ex);
                    }
                } catch (StorageException | WRuntimeException | NestedRuntimeException ex) {
                    throw new JobExecutionException(String.format("Job execution failed (partyId='%s', shopId='%s', " +
                                    "scheduledJobContext='%s'), retry",
                            partyId, shopId, scheduledJobContext), ex);
                } catch (Exception ex) {
                    throw new JobExecutionException(
                            String.format("Job execution failed (partyId='%s', shopId='%s', " +
                                            "scheduledJobContext='%s'), stop triggers",
                                    partyId, shopId, scheduledJobContext), ex);
                }
            }
            return ByteBuffer.wrap(scheduleJobSerializer.writeByte(scheduledJobContext));
        } catch (Exception ex) {
            log.error("Error was received when performing a scheduled task", ex);
            throw new IllegalStateException(String.format("Execute job '%d' failed", scheduledJobContext.getJobId()));
        }
    }
}
