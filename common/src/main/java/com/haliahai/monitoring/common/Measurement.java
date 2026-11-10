package com.haliahai.monitoring.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Measurement(
        String warehouseId,
        String sensorId,
        SensorType type,
        BigDecimal value,
        Instant timestamp) {

    public Measurement {
        requireNonBlank(warehouseId, "warehouseId");
        requireNonBlank(sensorId, "sensorId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
