package com.haliahai.monitoring.central.evaluator;

import com.haliahai.monitoring.central.alarm.Alarm;
import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Alarm only when value > threshold; equal is not an alarm.
public final class ThresholdEvaluator {

    private final Map<SensorType, BigDecimal> thresholds;

    public ThresholdEvaluator(Map<SensorType, BigDecimal> thresholds) {
        Objects.requireNonNull(thresholds, "thresholds");
        var missing = Arrays.stream(SensorType.values()).filter(type -> thresholds.get(type) == null).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing alarm threshold for sensor types " + missing);
        }
        this.thresholds = Map.copyOf(new EnumMap<>(thresholds));
    }

    public Optional<Alarm> evaluate(Measurement measurement) {
        var threshold = thresholds.get(measurement.type());
        return measurement.value().compareTo(threshold) > 0
                ? Optional.of(new Alarm(measurement, threshold))
                : Optional.empty();
    }
}
