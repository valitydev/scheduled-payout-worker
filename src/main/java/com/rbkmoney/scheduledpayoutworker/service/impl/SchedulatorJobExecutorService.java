package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.schedule.ContextValidationResponse;
import com.rbkmoney.damsel.schedule.ExecuteJobRequest;
import com.rbkmoney.damsel.schedule.ScheduledJobExecutorSrv;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
public class SchedulatorJobExecutorService implements ScheduledJobExecutorSrv.Iface {

    @Override
    public ContextValidationResponse validateExecutionContext(ByteBuffer byteBuffer) throws TException {
        //TODO: Implement during JD-371
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public ByteBuffer executeJob(ExecuteJobRequest executeJobRequest) throws TException {
        //TODO: Implement during JD-371
        throw new UnsupportedOperationException("Not implemented!");
    }
}
