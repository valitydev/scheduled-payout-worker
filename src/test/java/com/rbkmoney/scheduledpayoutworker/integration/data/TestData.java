package com.rbkmoney.scheduledpayoutworker.integration.data;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.machinegun.msgpack.Value;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.List;

public class TestData {

    private static final ThreadLocal<TSerializer> serializerLocal =
            ThreadLocal.withInitial(() -> new TSerializer(new TBinaryProtocol.Factory()));

    public static SinkEvent createInvoice() {
        SinkEvent sinkEvent = new SinkEvent();
        try {
            sinkEvent.setEvent(createMachineEvent(serializerLocal.get().serialize(createEventPayload())));
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        return sinkEvent;
    }

    public static EventPayload createEventPayload() {
        EventPayload payload = new EventPayload();
        payload.setCustomerChanges(List.of());

        return payload;
    }

    public static MachineEvent createMachineEvent(byte[] data) {
        Value value = new Value();
        value.setBin(data);

        return new MachineEvent()
                .setSourceNs("Test")
                .setSourceId("Test")
                .setCreatedAt("Test")
                .setData(value);

    }

}
