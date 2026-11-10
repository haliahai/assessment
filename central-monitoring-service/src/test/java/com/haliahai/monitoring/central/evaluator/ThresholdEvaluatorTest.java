package com.haliahai.monitoring.central.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ThresholdEvaluatorTest {

    private final ThresholdEvaluator evaluator = new ThresholdEvaluator(Map.of(
            SensorType.TEMPERATURE, new BigDecimal("35"),
            SensorType.HUMIDITY, new BigDecimal("50")));

    @ParameterizedTest(name = "{0} = {1} -> alarm: {2}")
    @CsvSource({
            "TEMPERATURE, 34.99, false",
            "TEMPERATURE, 35,    false",
            "TEMPERATURE, 35.01, true",
            "HUMIDITY,    49.99, false",
            "HUMIDITY,    50,    false",
            "HUMIDITY,    50.01, true",
    })
    void alarmsOnlyWhenValueIsStrictlyAboveThreshold(SensorType type, BigDecimal value, boolean expectAlarm) {
        assertThat(evaluator.evaluate(measurement(type, value)).isPresent()).isEqualTo(expectAlarm);
    }

    @Test
    void usesConfiguredThresholds() {
        var custom = new ThresholdEvaluator(Map.of(
                SensorType.TEMPERATURE, new BigDecimal("20"),
                SensorType.HUMIDITY, new BigDecimal("90")));

        assertThat(custom.evaluate(measurement(SensorType.TEMPERATURE, new BigDecimal("21")))).isPresent();
        assertThat(custom.evaluate(measurement(SensorType.HUMIDITY, new BigDecimal("60")))).isEmpty();
    }

    private static Measurement measurement(SensorType type, BigDecimal value) {
        return new Measurement("w1", "s1", type, value, Instant.parse("2026-01-01T10:00:00Z"));
    }
}
