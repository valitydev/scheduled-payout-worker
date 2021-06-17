package com.rbkmoney.scheduledpayoutworker.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public class TestContainers {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String POSTGRESQL_VERSION = "9.6";
    private static final String CONFLUENT_IMAGE_NAME = "confluentinc/cp-kafka";
    private static final String CONFLUENT_PLATFORM_VERSION = "latest";

    protected static final PostgreSQLContainer DB;

    protected static final KafkaContainer KAFKA;

    static {
        DB = new PostgreSQLContainer(DockerImageName
                .parse(POSTGRESQL_IMAGE_NAME)
                .withTag(POSTGRESQL_VERSION));

        KAFKA = new KafkaContainer(DockerImageName
                .parse(CONFLUENT_IMAGE_NAME)
                .withTag(CONFLUENT_PLATFORM_VERSION))
                .withEmbeddedZookeeper();

        DB.start();
        KAFKA.start();
    }


    @DynamicPropertySource
    static void connectionConfigs(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
        registry.add("flyway.url", DB::getJdbcUrl);
        registry.add("flyway.user", DB::getUsername);
        registry.add("flyway.password", DB::getPassword);
        registry.add("kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

}
