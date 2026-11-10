package com.haliahai.monitoring.warehouse.parser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MeasurementParserTest {

    private final MeasurementParser parser = new MeasurementParser();

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @CsvSource(delimiter = '|', value = {
            "sensor_id=t1; value=30         | t1 | 30",
            "'  sensor_id = h1 ;value=40  ' | h1 | 40",
            "sensor_id=t1; value=-12.5      | t1 | -12.5",
            "value=35.75; sensor_id=t1      | t1 | 35.75",
    })
    void parsesValidPayload(String payload, String sensorId, String value) {
        var reading = parser.parse(payload.getBytes(UTF_8));

        assertThat(reading.sensorId()).isEqualTo(sensorId);
        assertThat(reading.value()).isEqualByComparingTo(value);
    }

    @Test
    void toleratesTrailingNewlineFromNc() {
        assertThat(parser.parse("sensor_id=t1; value=30\n".getBytes(UTF_8)).value()).isEqualByComparingTo("30");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "''                                   | empty payload",
            "sensor_id=t1                         | expected keys",
            "sensor=t1; value=30                  | unexpected key 'sensor'",
            "sensor_id=t1; sensor_id=t2; value=30 | duplicate key 'sensor_id'",
            "hello world                          | field without '='",
            "sensor_id=t1; value=abc              | value is not a number",
            "sensor_id=t1; value=NaN              | value is not a number",
            "sensor_id=t1; value=Infinity         | value is not a number",
    })
    void rejectsMalformedPayload(String payload, String reason) {
        assertThatThrownBy(() -> parser.parse(payload.getBytes(UTF_8)))
                .isInstanceOf(MalformedMeasurementException.class)
                .hasMessageContaining(reason);
    }

    @Test
    void rejectsOversizedPayload() {
        var payload = ("sensor_id=t1; value=30" + " ".repeat(MeasurementParser.MAX_PAYLOAD_BYTES)).getBytes(UTF_8);

        assertThatThrownBy(() -> parser.parse(payload)).hasMessageContaining("exceeds");
    }

    @Test
    void rejectsNonUtf8Bytes() {
        byte[] payload = {'s', 'e', 'n', (byte) 0xC3, (byte) 0x28, '=', '1'};

        assertThatThrownBy(() -> parser.parse(payload)).hasMessageContaining("not valid UTF-8");
    }
}
