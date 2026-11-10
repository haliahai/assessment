package com.haliahai.monitoring.warehouse.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haliahai.monitoring.common.Measurement;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

public final class KafkaMeasurementPublisher implements MeasurementPublisher {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaMeasurementPublisher(KafkaSender<String, String> sender, ObjectMapper objectMapper, String topic) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.topic = Objects.requireNonNull(topic, "topic");
    }

    @Override
    public Mono<Void> publish(Measurement measurement) {
        return Mono.fromCallable(() -> toRecord(measurement))
                .flatMap(record -> sender.send(Mono.just(record)).then())
                // send() can block on metadata when Kafka is down - keep it off the Netty event loop
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SenderRecord<String, String, String> toRecord(Measurement measurement) throws JsonProcessingException {
        var key = measurement.warehouseId() + ":" + measurement.sensorId();
        var json = objectMapper.writeValueAsString(measurement);
        return SenderRecord.create(new ProducerRecord<>(topic, key, json), key);
    }
}
