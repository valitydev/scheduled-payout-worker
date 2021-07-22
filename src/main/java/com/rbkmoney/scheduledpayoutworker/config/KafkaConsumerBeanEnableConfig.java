package com.rbkmoney.scheduledpayoutworker.config;

import com.rbkmoney.scheduledpayoutworker.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.scheduledpayoutworker.poller.listener.PartyManagementKafkaListener;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementEventService;
import com.rbkmoney.scheduledpayoutworker.service.PaymentProcessingEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerBeanEnableConfig {

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.invoice.enabled", havingValue = "true")
    public InvoicingKafkaListener paymentEventsKafkaListener(
            PaymentProcessingEventService paymentEventService,
            ConversionService conversionService) {
        return new InvoicingKafkaListener(paymentEventService, conversionService);
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.party-management.enabled", havingValue = "true")
    public PartyManagementKafkaListener partyManagementKafkaListener(
            PartyManagementEventService partyManagementEventService,
            ConversionService conversionService) {
        return new PartyManagementKafkaListener(partyManagementEventService, conversionService);
    }

}
