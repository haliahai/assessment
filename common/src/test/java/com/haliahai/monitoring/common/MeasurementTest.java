package com.haliahai.monitoring.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"1E+999999999", "1E-999999999", "1234567890", "-1234567890", "0.1234567"})
    void rejectsValueOutsideSupportedRange(String value) {
        assertThatThrownBy(() -> new Measurement("w1", "t1", SensorType.TEMPERATURE, new BigDecimal(value), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999.999999", "-999999999.999999", "0", "36.5"})
    void acceptsValueInsideSupportedRange(String value) {
        var measurement = new Measurement("w1", "t1", SensorType.TEMPERATURE, new BigDecimal(value), NOW);

        assertThat(measurement.value()).isEqualByComparingTo(value);
    }

    @Test
    void rejectsHugeExponentFromJson() {
        var json = """
                {"warehouseId":"w1","sensorId":"t1","type":"TEMPERATURE","value":1E+999999999,
                 "timestamp":"2026-01-01T10:00:00Z"}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, Measurement.class))
                .isInstanceOf(JsonMappingException.class);
    }
}
