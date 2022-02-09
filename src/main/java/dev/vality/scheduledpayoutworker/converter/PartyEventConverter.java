package dev.vality.scheduledpayoutworker.converter;

import dev.vality.damsel.payment_processing.PartyEventData;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.exception.ParseException;
import dev.vality.scheduledpayoutworker.serde.impl.BinaryEventDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PartyEventConverter implements Converter<MachineEvent, PartyEventData> {

    private final BinaryEventDeserializer<PartyEventData> deserializer = new BinaryEventDeserializer<>();

    @Override
    @NonNull
    public PartyEventData convert(@NonNull MachineEvent event) {
        PartyEventData data = new PartyEventData();
        try {
            deserializer.deserialize(event.getData().getBin(), data);
        } catch (Exception e) {
            log.error("Exception when parse message e: ", e);
            throw new ParseException();
        }

        return data;
    }
}