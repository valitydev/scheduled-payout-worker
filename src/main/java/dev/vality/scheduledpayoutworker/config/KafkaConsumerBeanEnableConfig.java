package dev.vality.scheduledpayoutworker.config;

import dev.vality.scheduledpayoutworker.poller.listener.PartyManagementKafkaListener;
import dev.vality.scheduledpayoutworker.service.PartyManagementEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerBeanEnableConfig {

    @Bean
    @ConditionalOnProperty(value = "spring.kafka.topics.party-management.enabled", havingValue = "true")
    public PartyManagementKafkaListener partyManagementKafkaListener(
            PartyManagementEventService partyManagementEventService,
            ConversionService conversionService) {
        return new PartyManagementKafkaListener(partyManagementEventService, conversionService);
    }

}
