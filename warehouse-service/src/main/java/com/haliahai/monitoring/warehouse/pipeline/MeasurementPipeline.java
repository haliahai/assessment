package com.haliahai.monitoring.warehouse.pipeline;

import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import com.haliahai.monitoring.warehouse.parser.MalformedMeasurementException;
import com.haliahai.monitoring.warehouse.parser.MeasurementParser;
import com.haliahai.monitoring.warehouse.publisher.MeasurementPublisher;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Bad datagrams and failed publishes are logged and dropped, the stream keeps going.
// UDP can't be slowed down, so if publishing lags we buffer bufferSize datagrams and drop the rest.
public final class MeasurementPipeline {

    private static final Logger log = LoggerFactory.getLogger(MeasurementPipeline.class);

    private final String warehouseId;
    private final MeasurementParser parser;
    private final MeasurementPublisher publisher;
    private final Clock clock;
    private final int bufferSize;
    private final int maxInFlight;

    public MeasurementPipeline(
            String warehouseId,
            MeasurementParser parser,
            MeasurementPublisher publisher,
            Clock clock,
            int bufferSize,
            int maxInFlight) {
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.bufferSize = bufferSize;
        this.maxInFlight = maxInFlight;
    }

    public Flux<Measurement> process(SensorType type, Flux<byte[]> datagrams) {
        return datagrams
                .onBackpressureBuffer(
                        bufferSize,
                        dropped -> log.warn("Dropping {} datagram: buffer of {} is full", type, bufferSize),
                        BufferOverflowStrategy.DROP_LATEST)
                .<Measurement>handle((payload, sink) -> toMeasurement(type, payload).ifPresent(sink::next))
                .flatMap(this::publish, maxInFlight);
    }

    private Optional<Measurement> toMeasurement(SensorType type, byte[] payload) {
        try {
            var reading = parser.parse(payload);
            return Optional.of(new Measurement(warehouseId, reading.sensorId(), type, reading.value(), clock.instant()));
        } catch (MalformedMeasurementException e) {
            log.warn("Dropping malformed {} datagram ({}): '{}'", type, e.reason(), e.payloadPreview());
            return Optional.empty();
        }
    }

    private Mono<Measurement> publish(Measurement measurement) {
        return publisher.publish(measurement)
                .thenReturn(measurement)
                .doOnNext(published -> log.debug("Published {}", published))
                .onErrorResume(e -> {
                    log.error("Failed to publish {}, dropping it", measurement, e);
                    return Mono.empty();
                });
    }
}
