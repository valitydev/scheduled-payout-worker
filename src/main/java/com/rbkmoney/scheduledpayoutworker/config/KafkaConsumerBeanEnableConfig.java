package com.rbkmoney.scheduledpayoutworker.config;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.scheduledpayoutworker.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.scheduledpayoutworker.service.PaymentProcessingEventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerBeanEnableConfig {

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.invoice.enabled", havingValue = "true")
    public InvoicingKafkaListener paymentEventsKafkaListener(
            PaymentProcessingEventService paymentEventService,
            MachineEventParser<EventPayload> parser) {
        return new InvoicingKafkaListener(paymentEventService, parser);
    }

}
