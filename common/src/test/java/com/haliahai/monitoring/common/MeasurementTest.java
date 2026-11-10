package com.haliahai.monitoring.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MeasurementTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void roundTripsThroughJson() throws Exception {
        var measurement = new Measurement("w1", "t1", SensorType.TEMPERATURE, new BigDecimal("35.5"), NOW);

        var json = mapper.writeValueAsString(measurement);

        assertThat(mapper.readValue(json, Measurement.class)).isEqualTo(measurement);
    }

    @Test
    void toleratesUnknownFieldsForAdditiveContractChanges() throws Exception {
        var json = """
                {"warehouseId":"w1","sensorId":"h1","type":"HUMIDITY","value":40,
                 "timestamp":"2026-01-01T10:00:00Z","firmware":"2.1"}
                """;

        var measurement = mapper.readValue(json, Measurement.class);

        assertThat(measurement.sensorId()).isEqualTo("h1");
        assertThat(measurement.value()).isEqualByComparingTo("40");
    }
}
