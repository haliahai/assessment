package com.haliahai.monitoring.central.alarm;

public interface AlarmSink {

    void raise(Alarm alarm);
}
