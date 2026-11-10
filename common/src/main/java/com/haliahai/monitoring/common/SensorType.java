package com.haliahai.monitoring.common;

public enum SensorType {
    TEMPERATURE("°C"),
    HUMIDITY("%");

    private final String unit;

    SensorType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
