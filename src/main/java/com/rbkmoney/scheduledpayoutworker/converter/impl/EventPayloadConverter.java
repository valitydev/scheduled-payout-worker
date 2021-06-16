package com.rbkmoney.scheduledpayoutworker.converter.impl;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.scheduledpayoutworker.converter.BinaryConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventPayloadConverter implements BinaryConverter<EventPayload> {

    ThreadLocal<TDeserializer> deserializerLocal =
            ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public EventPayload convert(byte[] bin) {
        EventPayload event = new EventPayload();
        try {
            deserializerLocal.get().deserialize(event, bin);
        } catch (TException e) {
            log.error("BinaryConverterImpl e: ", e);
        }
        return event;
    }
}
