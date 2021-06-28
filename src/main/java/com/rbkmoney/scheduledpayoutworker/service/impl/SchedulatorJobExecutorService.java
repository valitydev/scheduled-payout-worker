package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.schedule.*;
import com.rbkmoney.scheduledpayoutworker.model.ScheduledJobContext;
import com.rbkmoney.scheduledpayoutworker.serde.impl.ScheduledJobSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulatorJobExecutorService implements ScheduledJobExecutorSrv.Iface {

    private final ScheduledJobSerializer scheduleJobSerializer;

    @Override
    public ContextValidationResponse validateExecutionContext(ByteBuffer byteBuffer) throws TException {
        ScheduledJobContext scheduledJobContext = scheduleJobSerializer.read(byteBuffer.array());
        if (scheduledJobContext.getJobId() == null) {
            throw new IllegalArgumentException("Job id cannot be null!");
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
            //TODO: JD-371: Implement payout.
            return ByteBuffer.wrap(scheduleJobSerializer.writeByte(scheduledJobContext));
        } catch (Exception ex) {
            log.error("Error was received when performing a scheduled task", ex);
            throw new IllegalStateException(String.format("Execute job '%d' failed", scheduledJobContext.getJobId()));
        }
    }
}
