package com.haliahai.monitoring.warehouse.parser;

import com.haliahai.monitoring.common.Measurement;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

// Parses "sensor_id=t1; value=30". Extra whitespace, trailing newline and key order are tolerated.
public final class MeasurementParser {

    public static final int MAX_PAYLOAD_BYTES = 256;

    private static final String SENSOR_ID_KEY = "sensor_id";
    private static final String VALUE_KEY = "value";
    private static final Set<String> EXPECTED_KEYS = Set.of(SENSOR_ID_KEY, VALUE_KEY);
    private static final Pattern SENSOR_ID = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Pattern VALUE = Pattern.compile(
            "[+-]?\\d{1," + Measurement.MAX_INTEGER_DIGITS + "}(\\.\\d{1," + Measurement.MAX_FRACTION_DIGITS + "})?");
    private static final int PREVIEW_CHARS = 64;

    public SensorReading parse(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw malformed("payload exceeds " + MAX_PAYLOAD_BYTES + " bytes", payload);
        }
        var text = decode(payload).strip();
        if (text.isEmpty()) {
            throw malformed("empty payload", payload);
        }
        var fields = fields(text, payload);
        return new SensorReading(sensorId(fields.get(SENSOR_ID_KEY), payload), value(fields.get(VALUE_KEY), payload));
    }

    private static String decode(byte[] payload) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new MalformedMeasurementException("payload is not valid UTF-8", preview(payload), e);
        }
    }

    private static Map<String, String> fields(String text, byte[] payload) {
        var fields = new HashMap<String, String>();
        for (var field : text.split(";", -1)) {
            var separator = field.indexOf('=');
            if (separator < 0) {
                throw malformed("field without '=': '" + field.strip() + "'", payload);
            }
            var key = field.substring(0, separator).strip();
            var value = field.substring(separator + 1).strip();
            if (!EXPECTED_KEYS.contains(key)) {
                throw malformed("unexpected key '" + key + "'", payload);
            }
            if (fields.put(key, value) != null) {
                throw malformed("duplicate key '" + key + "'", payload);
            }
        }
        if (!fields.keySet().equals(EXPECTED_KEYS)) {
            throw malformed("expected keys " + EXPECTED_KEYS + " but got " + fields.keySet(), payload);
        }
        return fields;
    }

    private static String sensorId(String raw, byte[] payload) {
        if (!SENSOR_ID.matcher(raw).matches()) {
            throw malformed("invalid sensor_id '" + raw + "'", payload);
        }
        return raw;
    }

    private static BigDecimal value(String raw, byte[] payload) {
        if (!VALUE.matcher(raw).matches()) {
            throw malformed("value is not a plain decimal (max " + Measurement.MAX_INTEGER_DIGITS + " integer and "
                    + Measurement.MAX_FRACTION_DIGITS + " fraction digits): '" + raw + "'", payload);
        }
        return new BigDecimal(raw);
    }

    private static MalformedMeasurementException malformed(String reason, byte[] payload) {
        return new MalformedMeasurementException(reason, preview(payload));
    }

    // no control chars, so a payload can't fake log lines
    static String preview(byte[] payload) {
        var text = new String(payload, StandardCharsets.UTF_8);
        var truncated = text.length() > PREVIEW_CHARS ? text.substring(0, PREVIEW_CHARS) + "..." : text;
        return truncated.replaceAll("\\p{Cntrl}", "?");
    }
}
