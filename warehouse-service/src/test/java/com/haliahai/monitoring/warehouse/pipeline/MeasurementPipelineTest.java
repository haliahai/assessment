package com.haliahai.monitoring.warehouse.pipeline;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import com.haliahai.monitoring.warehouse.parser.MeasurementParser;
import com.haliahai.monitoring.warehouse.publisher.RecordingMeasurementPublisher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class MeasurementPipelineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final RecordingMeasurementPublisher publisher = new RecordingMeasurementPublisher();
    private final MeasurementPipeline pipeline = pipelineWith(publisher);

    @Test
    void enrichesReadingWithWarehouseTypeAndTimestamp() {
        StepVerifier.create(pipeline.process(SensorType.TEMPERATURE, datagrams("sensor_id=t1; value=30")))
                .expectNext(new Measurement("w1", "t1", SensorType.TEMPERATURE, new BigDecimal("30"), NOW))
                .verifyComplete();

        assertThat(publisher.published()).hasSize(1);
    }

    @Test
    void malformedDatagramDoesNotStopTheStream() {
        var input = datagrams("sensor_id=h1; value=40", "garbage", "sensor_id=h2; value=NaN", "sensor_id=h3; value=55");

        StepVerifier.create(pipeline.process(SensorType.HUMIDITY, input).map(Measurement::sensorId))
                .expectNext("h1", "h3")
                .verifyComplete();
    }

    @Test
    void publishFailureDropsOnlyThatMeasurement() {
        var failing = new RecordingMeasurementPublisher(m -> m.sensorId().equals("t2"));
        var input = datagrams("sensor_id=t1; value=1", "sensor_id=t2; value=2", "sensor_id=t3; value=3");

        StepVerifier.create(pipelineWith(failing).process(SensorType.TEMPERATURE, input).map(Measurement::sensorId))
                .expectNext("t1", "t3")
                .verifyComplete();

        assertThat(failing.published()).extracting(Measurement::sensorId).containsExactly("t1", "t3");
    }

    private static MeasurementPipeline pipelineWith(RecordingMeasurementPublisher publisher) {
        return new MeasurementPipeline("w1", new MeasurementParser(), publisher, CLOCK, 16, 1);
    }

    private static Flux<byte[]> datagrams(String... payloads) {
        return Flux.just(payloads).map(p -> p.getBytes(UTF_8));
    }
}
