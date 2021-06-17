package com.rbkmoney.scheduledpayoutworker.kafka;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.scheduledpayoutworker.config.KafkaConfig;
import com.rbkmoney.scheduledpayoutworker.converter.impl.EventPayloadConverter;
import com.rbkmoney.scheduledpayoutworker.integration.AbstractKafkaIntegrationTest;
import com.rbkmoney.scheduledpayoutworker.poller.listener.InvoicingKafkaListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ContextConfiguration(classes = {KafkaConfig.class, InvoicingKafkaListener.class})
public class InvoiceKafkaListenerTest extends AbstractKafkaIntegrationTest {

    @Value("${kafka.topics.invoice.id}")
    public String topic;

    @MockBean
    private EventPayloadConverter parser;

    @Test
    public void listenChanges() {
        when(parser.convert(any())).thenReturn(EventPayload.invoice_changes(List.of(new InvoiceChange())));

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createTestMachineEvent());

        writeToTopic(topic, sinkEvent);

        verify(parser, timeout(KAFKA_SYNC_TIME).times(1)).convert(any());
    }

}
