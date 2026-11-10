package com.haliahai.monitoring.warehouse.parser;

import java.math.BigDecimal;
import java.util.Objects;

public record SensorReading(String sensorId, BigDecimal value) {

    public SensorReading {
        Objects.requireNonNull(sensorId, "sensorId");
        Objects.requireNonNull(value, "value");
    }
}
