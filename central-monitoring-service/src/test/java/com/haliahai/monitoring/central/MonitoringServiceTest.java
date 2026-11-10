package com.haliahai.monitoring.central;

import static org.assertj.core.api.Assertions.assertThat;

import com.haliahai.monitoring.central.alarm.Alarm;
import com.haliahai.monitoring.central.alarm.RecordingAlarmSink;
import com.haliahai.monitoring.central.evaluator.ThresholdEvaluator;
import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

class MonitoringServiceTest {

    private final ThresholdEvaluator evaluator = new ThresholdEvaluator(Map.of(
            SensorType.TEMPERATURE, new BigDecimal("35"),
            SensorType.HUMIDITY, new BigDecimal("50")));
    private final RecordingAlarmSink sink = new RecordingAlarmSink();

    @Test
    void raisesAlarmsOnlyForMeasurementsAboveThreshold() {
        var source = Flux.just(
                measurement("t1", SensorType.TEMPERATURE, "30"),
                measurement("t2", SensorType.TEMPERATURE, "35"),
                measurement("t3", SensorType.TEMPERATURE, "36"),
                measurement("h1", SensorType.HUMIDITY, "50"),
                measurement("h2", SensorType.HUMIDITY, "51"));
        var service = new MonitoringService(() -> source, evaluator, sink, Retry.max(0));

        StepVerifier.create(service.alarms().map(alarm -> alarm.measurement().sensorId()))
                .expectNext("t3", "h2")
                .verifyComplete();

        assertThat(sink.alarms()).extracting(alarm -> alarm.measurement().sensorId()).containsExactly("t3", "h2");
    }

    @Test
    void resubscribesAfterSourceFailure() {
        var subscriptions = new AtomicInteger();
        var service = new MonitoringService(
                () -> subscriptions.incrementAndGet() == 1
                        ? Flux.<Measurement>error(new IllegalStateException("broker down"))
                        : Flux.just(measurement("t1", SensorType.TEMPERATURE, "40")),
                evaluator, sink, Retry.max(1));

        StepVerifier.create(service.alarms().map(Alarm::threshold))
                .assertNext(threshold -> assertThat(threshold).isEqualByComparingTo("35"))
                .verifyComplete();

        assertThat(subscriptions).hasValue(2);
    }

    private static Measurement measurement(String sensorId, SensorType type, String value) {
        return new Measurement("w1", sensorId, type, new BigDecimal(value), Instant.parse("2026-01-01T10:00:00Z"));
    }
}
