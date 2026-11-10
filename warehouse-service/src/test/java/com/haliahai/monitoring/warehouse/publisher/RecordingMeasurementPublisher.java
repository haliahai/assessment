package com.haliahai.monitoring.warehouse.publisher;

import com.haliahai.monitoring.common.Measurement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

public final class RecordingMeasurementPublisher implements MeasurementPublisher {

    private final List<Measurement> published = new CopyOnWriteArrayList<>();
    private final Predicate<Measurement> failWhen;

    public RecordingMeasurementPublisher() {
        this(measurement -> false);
    }

    public RecordingMeasurementPublisher(Predicate<Measurement> failWhen) {
        this.failWhen = failWhen;
    }

    @Override
    public Mono<Void> publish(Measurement measurement) {
        return Mono.defer(() -> {
            if (failWhen.test(measurement)) {
                return Mono.error(new IllegalStateException("broker unavailable"));
            }
            published.add(measurement);
            return Mono.empty();
        });
    }

    public List<Measurement> published() {
        return List.copyOf(published);
    }
}
