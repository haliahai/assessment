package com.haliahai.monitoring.central.alarm;

import com.haliahai.monitoring.common.Measurement;
import java.math.BigDecimal;
import java.util.Objects;

public record Alarm(Measurement measurement, BigDecimal threshold) {

    public Alarm {
        Objects.requireNonNull(measurement, "measurement");
        Objects.requireNonNull(threshold, "threshold");
    }
}
