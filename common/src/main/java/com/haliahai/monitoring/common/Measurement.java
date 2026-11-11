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

    public static final int MAX_INTEGER_DIGITS = 9;
    public static final int MAX_FRACTION_DIGITS = 6;

    public Measurement {
        requireNonBlank(warehouseId, "warehouseId");
        requireNonBlank(sensorId, "sensorId");
        Objects.requireNonNull(type, "type");
        requireSupportedRange(value);
        Objects.requireNonNull(timestamp, "timestamp");
    }

    private static void requireSupportedRange(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        var fractionDigits = Math.max(value.scale(), 0);
        var integerDigits = (long) value.precision() - value.scale();
        if (fractionDigits > MAX_FRACTION_DIGITS || integerDigits > MAX_INTEGER_DIGITS) {
            throw new IllegalArgumentException("value " + value + " exceeds " + MAX_INTEGER_DIGITS
                    + " integer / " + MAX_FRACTION_DIGITS + " fraction digits");
        }
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
