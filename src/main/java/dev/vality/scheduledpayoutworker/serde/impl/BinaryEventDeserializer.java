package dev.vality.scheduledpayoutworker.serde.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

@Slf4j
public class BinaryEventDeserializer<T extends TBase> {

    ThreadLocal<TDeserializer> deserializerLocal =
            ThreadLocal.withInitial(() -> {
                try {
                    return new TDeserializer(new TBinaryProtocol.Factory());
                } catch (TTransportException e) {
                    throw new RuntimeException(e);
                }
            });

    public void deserialize(byte[] bin, T reply) {
        try {
            deserializerLocal.get().deserialize(reply, bin);
        } catch (TException e) {
            log.error("BinaryEventDeserializer e: ", e);
        }
    }

}
