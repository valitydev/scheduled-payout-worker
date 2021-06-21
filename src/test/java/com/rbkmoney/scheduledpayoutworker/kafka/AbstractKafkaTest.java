package com.rbkmoney.scheduledpayoutworker.kafka;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.scheduledpayoutworker.AbstractIntegrationTest;
import com.rbkmoney.scheduledpayoutworker.ScheduledPayoutWorkerApplication;
import com.rbkmoney.scheduledpayoutworker.serde.impl.kafka.MachineEventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = ScheduledPayoutWorkerApplication.class,
        initializers = AbstractKafkaTest.Initializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
public abstract class AbstractKafkaTest extends AbstractIntegrationTest {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final long KAFKA_SYNC_TIME = 5000L;

    private static final String CONFLUENT_IMAGE_NAME = "confluentinc/cp-kafka";
    private static final String CONFLUENT_PLATFORM_VERSION = "latest";

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName
            .parse(CONFLUENT_IMAGE_NAME)
            .withTag(CONFLUENT_PLATFORM_VERSION))
            .withEmbeddedZookeeper();

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues
                    .of("kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                            "kafka.ssl.enabled=false",
                            "kafka.client-id=scheduled-payout-worker",
                            "kafka.consumer.group-id=ScheduledPayoutWorkerGroup",
                            "kafka.consumer.concurrency=1",
                            "kafka.consumer.client-id=id1",
                            "kafka.consumer.max-poll-records=20",
                            "kafka.consumer.max-poll-interval-ms=30000",
                            "kafka.consumer.connections-max-idle-ms=30000",
                            "kafka.consumer.session-timeout-ms=30000",
                            "kafka.consumer.enable-auto-commit=false",
                            "kafka.consumer.auto-offset-reset=earliest",
                            "kafka.consumer.disconnect-backoff-ms=5000",
                            "kafka.consumer.retry-backoff-ms=1000",
                            "kafka.topics.invoice.id=mg-invoice",
                            "kafka.topics.invoice.enabled=true",
                            "kafka.retry-policy.maxAttempts=10")
                    .applyTo(configurableApplicationContext);
        }
    }

    private Producer<String, SinkEvent> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "client_id");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MachineEventSerializer.class);
        return new KafkaProducer<>(props);
    }

    public void writeToTopic(String topic, SinkEvent sinkEvent) {
        Producer<String, SinkEvent> producer = createProducer();
        ProducerRecord<String, SinkEvent> producerRecord = new ProducerRecord<>(topic, "", sinkEvent);
        try {
            producer.send(producerRecord).get();
        } catch (Exception e) {
            log.error("KafkaAbstractTest initialize e: ", e);
        }
        producer.close();
    }

    public static MachineEvent createTestMachineEvent() {
        MachineEvent message = new MachineEvent();
        com.rbkmoney.machinegun.msgpack.Value data = new com.rbkmoney.machinegun.msgpack.Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        message.setEventId(1L);
        message.setSourceNs("sad");
        message.setSourceId("sda");
        message.setData(data);
        return message;
    }
}
