package com.haliahai.monitoring.central.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class LoggingAlarmSink implements AlarmSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlarmSink.class);
    private static final Marker ALARM = MarkerFactory.getMarker("ALARM");

    @Override
    public void raise(Alarm alarm) {
        var m = alarm.measurement();
        var unit = m.type().unit();
        log.warn(ALARM, "ALARM {} threshold exceeded: warehouse={} sensor={} value={}{} threshold={}{} measuredAt={}",
                m.type(), m.warehouseId(), m.sensorId(),
                m.value().toPlainString(), unit, alarm.threshold().toPlainString(), unit, m.timestamp());
    }
}
