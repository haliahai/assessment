package com.haliahai.monitoring.central.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingAlarmSinkTest {

    @Test
    void logsAlarmWithAllContext(CapturedOutput output) {
        var measurement = new Measurement(
                "w1", "t1", SensorType.TEMPERATURE, new BigDecimal("36.5"), Instant.parse("2026-01-01T10:00:00Z"));

        new LoggingAlarmSink().raise(new Alarm(measurement, new BigDecimal("35")));

        assertThat(output.getOut())
                .contains("WARN")
                .contains("ALARM TEMPERATURE threshold exceeded")
                .contains("warehouse=w1", "sensor=t1", "value=36.5°C", "threshold=35°C", "measuredAt=2026-01-01T10:00:00Z");
    }
}
