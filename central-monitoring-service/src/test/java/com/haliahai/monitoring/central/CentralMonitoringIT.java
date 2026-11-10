package com.haliahai.monitoring.central;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.haliahai.monitoring.central.alarm.Alarm;
import com.haliahai.monitoring.central.alarm.RecordingAlarmSink;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CentralMonitoringIT {

    private static final String TOPIC = "warehouse.measurements";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("monitoring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("monitoring.kafka.topic", () -> TOPIC);
    }

    @Autowired
    RecordingAlarmSink alarms;

    @Test
    void raisesAlarmForMeasurementAboveThresholdAndSurvivesPoisonMessage() throws Exception {
        try (var producer = new KafkaProducer<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
                new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(TOPIC, "w1:t1", json("t1", "TEMPERATURE", "30"))).get();
            producer.send(new ProducerRecord<>(TOPIC, "w1:x", "{not json")).get();
            producer.send(new ProducerRecord<>(TOPIC, "w1:h1", json("h1", "HUMIDITY", "50"))).get();
            producer.send(new ProducerRecord<>(TOPIC, "w1:t2", json("t2", "TEMPERATURE", "36"))).get();
            producer.send(new ProducerRecord<>(TOPIC, "w1:h2", json("h2", "HUMIDITY", "51"))).get();
        }

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(alarms.alarms())
                .extracting(alarm -> alarm.measurement().sensorId())
                .containsExactlyInAnyOrder("t2", "h2"));
        assertThat(alarms.alarms()).extracting(Alarm::threshold)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("35"), new BigDecimal("50"));
    }

    private static String json(String sensorId, String type, String value) {
        return """
                {"warehouseId":"w1","sensorId":"%s","type":"%s","value":%s,"timestamp":"2026-01-01T10:00:00Z"}"""
                .formatted(sensorId, type, value);
    }

    @TestConfiguration
    static class RecordingAlarmSinkConfig {

        @Bean
        @Primary
        RecordingAlarmSink recordingAlarmSink() {
            return new RecordingAlarmSink();
        }
    }
}
