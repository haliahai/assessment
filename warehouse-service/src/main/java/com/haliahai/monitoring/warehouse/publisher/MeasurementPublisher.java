package com.haliahai.monitoring.warehouse.publisher;

import com.haliahai.monitoring.common.Measurement;
import reactor.core.publisher.Mono;

public interface MeasurementPublisher {

    Mono<Void> publish(Measurement measurement);
}
