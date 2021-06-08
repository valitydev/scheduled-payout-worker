package com.rbkmoney.scheduledpayoutworker.serde;

import com.rbkmoney.kafka.common.serialization.ThriftSerializer;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MachineEventSerializer extends ThriftSerializer<SinkEvent> {

}
