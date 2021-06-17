package com.rbkmoney.scheduledpayoutworker.integration;

import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.scheduledpayoutworker.integration.config.TestKafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static com.rbkmoney.scheduledpayoutworker.integration.data.TestData.createInvoice;

@SpringBootTest
@Import(TestKafkaConfig.class)
public class ScheduledPayoutWorkerIntegrationTest extends TestContainers {

    @Autowired
    private KafkaTemplate<String, SinkEvent> producer;

    @Value("${kafka.topics.party-management.id}")
    private String partyTopic;

    @Value("${kafka.topics.invoice.id}")
    private String invoiceTopic;

    @Test
    public void readInvoicesAndProcessSchedulatorJobSuccessfully() {
        producer.send(invoiceTopic, createInvoice());
    }

}
