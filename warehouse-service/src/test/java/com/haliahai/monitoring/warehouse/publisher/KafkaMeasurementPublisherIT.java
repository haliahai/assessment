package com.haliahai.monitoring.warehouse.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Testcontainers(disabledWithoutDocker = true)
class KafkaMeasurementPublisherIT {

    private static final String TOPIC = "measurements-it";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void publishesMeasurementAsJsonKeyedByWarehouseAndSensor() throws Exception {
        var measurement = new Measurement(
                "w1", "t1", SensorType.TEMPERATURE, new BigDecimal("36.5"), Instant.parse("2026-01-01T10:00:00Z"));
        var sender = KafkaSender.<String, String>create(SenderOptions.create(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
        try {
            new KafkaMeasurementPublisher(sender, mapper, TOPIC).publish(measurement).block(Duration.ofSeconds(30));
        } finally {
            sender.close();
        }

        var record = consumeOne();
        assertThat(record.key()).isEqualTo("w1:t1");
        assertThat(mapper.readValue(record.value(), Measurement.class)).isEqualTo(measurement);
    }

    private ConsumerRecord<String, String> consumeOne() {
        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "publisher-it",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(TOPIC));
            var received = new ArrayList<ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(30))
                    .until(() -> {
                        consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                        return !received.isEmpty();
                    });
            return received.getFirst();
        }
    }
}
