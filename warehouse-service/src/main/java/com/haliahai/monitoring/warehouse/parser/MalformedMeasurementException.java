package com.haliahai.monitoring.warehouse.parser;

public class MalformedMeasurementException extends RuntimeException {

    private final String reason;
    private final String payloadPreview;

    MalformedMeasurementException(String reason, String payloadPreview) {
        this(reason, payloadPreview, null);
    }

    MalformedMeasurementException(String reason, String payloadPreview, Throwable cause) {
        super(reason + ": '" + payloadPreview + "'", cause);
        this.reason = reason;
        this.payloadPreview = payloadPreview;
    }

    public String reason() {
        return reason;
    }

    public String payloadPreview() {
        return payloadPreview;
    }
}
