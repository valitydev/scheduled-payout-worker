package com.rbkmoney.scheduledpayoutworker.serde;

import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MachineEventDeserializer extends AbstractThriftDeserializer<SinkEvent> {

    @Override
    public SinkEvent deserialize(String topic, byte[] data) {
        return this.deserialize(data, new SinkEvent());
    }

}
