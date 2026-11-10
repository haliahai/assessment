package com.haliahai.monitoring.central.consumer;

import com.haliahai.monitoring.common.Measurement;
import reactor.core.publisher.Flux;

public interface MeasurementSource {

    Flux<Measurement> measurements();
}
