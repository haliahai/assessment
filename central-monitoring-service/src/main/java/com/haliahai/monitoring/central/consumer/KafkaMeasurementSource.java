package com.haliahai.monitoring.central.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haliahai.monitoring.common.Measurement;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

// JSON is parsed here, not in a Kafka deserializer, so a bad record is skipped instead of breaking the consumer.
// Ack happens after sink.next() - downstream is synchronous, so the record is already evaluated (at-least-once).
public final class KafkaMeasurementSource implements MeasurementSource {

    private static final Logger log = LoggerFactory.getLogger(KafkaMeasurementSource.class);

    private final ReceiverOptions<String, String> options;
    private final ObjectMapper objectMapper;

    public KafkaMeasurementSource(ReceiverOptions<String, String> options, ObjectMapper objectMapper) {
        this.options = Objects.requireNonNull(options, "options");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public Flux<Measurement> measurements() {
        return KafkaReceiver.create(options)
                .receive()
                .handle((record, sink) -> {
                    try {
                        sink.next(objectMapper.readValue(record.value(), Measurement.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Skipping unreadable measurement at {}-{}@{}: {}",
                                record.topic(), record.partition(), record.offset(), e.getOriginalMessage());
                    } finally {
                        record.receiverOffset().acknowledge();
                    }
                });
    }
}
