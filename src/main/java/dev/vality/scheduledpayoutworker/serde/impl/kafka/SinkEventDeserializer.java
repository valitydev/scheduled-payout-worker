package dev.vality.scheduledpayoutworker.serde.impl.kafka;

import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SinkEventDeserializer extends AbstractThriftDeserializer<SinkEvent> {

    @Override
    public SinkEvent deserialize(String topic, byte[] data) {
        return this.deserialize(data, new SinkEvent());
    }

}