package com.rbkmoney.scheduledpayoutworker.converter.impl;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.scheduledpayoutworker.converter.BinaryConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PartyEventConverter implements BinaryConverter<PartyEventData> {

    ThreadLocal<TDeserializer> deserializerLocal =
            ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public PartyEventData convert(byte[] bin) {
        PartyEventData event = new PartyEventData();
        try {
            deserializerLocal.get().deserialize(new PartyEventData(), bin);
        } catch (TException e) {
            log.error("BinaryConverterImpl e: ", e);
        }
        return event;
    }
}
