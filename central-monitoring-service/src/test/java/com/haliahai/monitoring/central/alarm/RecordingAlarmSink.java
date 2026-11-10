package com.haliahai.monitoring.central.alarm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordingAlarmSink implements AlarmSink {

    private final List<Alarm> alarms = new CopyOnWriteArrayList<>();

    @Override
    public void raise(Alarm alarm) {
        alarms.add(alarm);
    }

    public List<Alarm> alarms() {
        return List.copyOf(alarms);
    }
}
