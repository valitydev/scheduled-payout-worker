package com.rbkmoney.scheduledpayoutworker.converter;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.exception.ParseException;
import com.rbkmoney.scheduledpayoutworker.serde.impl.BinaryEventDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SourceEventConverter implements Converter<MachineEvent, EventPayload> {

    private final BinaryEventDeserializer<EventPayload> deserializer = new BinaryEventDeserializer<>();

    @Override
    @NonNull
    public EventPayload convert(@NonNull MachineEvent message) {
        EventPayload eventPayload = new EventPayload();
        try {
            deserializer.deserialize(message.getData().getBin(), eventPayload);
        } catch (Exception e) {
            log.error("Exception when parse message e: ", e);
            throw new ParseException();
        }

        return eventPayload;
    }
}
