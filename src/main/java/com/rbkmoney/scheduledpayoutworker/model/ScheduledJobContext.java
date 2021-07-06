package com.rbkmoney.scheduledpayoutworker.model;

import lombok.Data;

@Data
public class ScheduledJobContext {

    private String jobId;
    private String partyId;
    private String shopId;

}
