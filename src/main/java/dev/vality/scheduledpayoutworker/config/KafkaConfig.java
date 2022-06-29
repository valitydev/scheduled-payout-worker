package dev.vality.scheduledpayoutworker.config;

import dev.vality.kafka.common.util.ExponentialBackOffDefaultErrorHandlerFactory;
import dev.vality.machinegun.eventsink.SinkEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.topics.party-management.concurrency}")
    private int partyConcurrency;

    @Bean
    public ConsumerFactory<String, SinkEvent> pmConsumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SinkEvent> pmContainerFactory(
            ConsumerFactory<String, SinkEvent> pmConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, SinkEvent>();
        factory.setConsumerFactory(pmConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(ExponentialBackOffDefaultErrorHandlerFactory.create());
        factory.setConcurrency(partyConcurrency);
        return factory;
    }

}
