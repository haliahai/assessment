package com.haliahai.monitoring.central;

import com.haliahai.monitoring.central.alarm.Alarm;
import com.haliahai.monitoring.central.alarm.AlarmSink;
import com.haliahai.monitoring.central.consumer.MeasurementSource;
import com.haliahai.monitoring.central.evaluator.ThresholdEvaluator;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

// source -> evaluator -> sink; resubscribes with backoff if the source fails (e.g. Kafka down)
public final class MonitoringService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MeasurementSource source;
    private final ThresholdEvaluator evaluator;
    private final AlarmSink alarmSink;
    private final Retry retry;
    private volatile Disposable subscription;

    public MonitoringService(MeasurementSource source, ThresholdEvaluator evaluator, AlarmSink alarmSink) {
        this(source, evaluator, alarmSink,
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)));
    }

    MonitoringService(MeasurementSource source, ThresholdEvaluator evaluator, AlarmSink alarmSink, Retry retry) {
        this.source = Objects.requireNonNull(source, "source");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.alarmSink = Objects.requireNonNull(alarmSink, "alarmSink");
        this.retry = Objects.requireNonNull(retry, "retry");
    }

    Flux<Alarm> alarms() {
        return Flux.defer(source::measurements)
                .doOnNext(measurement -> log.info("Received {}", measurement))
                .<Alarm>handle((measurement, sink) -> evaluator.evaluate(measurement).ifPresent(sink::next))
                .doOnNext(alarmSink::raise)
                .doOnError(e -> log.error("Measurement stream failed, resubscribing", e))
                .retryWhen(retry);
    }

    @Override
    public void start() {
        subscription = alarms().subscribe();
        log.info("Central monitoring started");
    }

    @Override
    public void stop() {
        var current = subscription;
        if (current != null) {
            current.dispose();
        }
        subscription = null;
    }

    @Override
    public boolean isRunning() {
        var current = subscription;
        return current != null && !current.isDisposed();
    }
}
