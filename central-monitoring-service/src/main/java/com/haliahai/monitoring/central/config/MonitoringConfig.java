package com.haliahai.monitoring.central.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haliahai.monitoring.central.MonitoringService;
import com.haliahai.monitoring.central.alarm.AlarmSink;
import com.haliahai.monitoring.central.alarm.LoggingAlarmSink;
import com.haliahai.monitoring.central.consumer.KafkaMeasurementSource;
import com.haliahai.monitoring.central.consumer.MeasurementSource;
import com.haliahai.monitoring.central.evaluator.ThresholdEvaluator;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;

@Configuration(proxyBeanMethods = false)
class MonitoringConfig {

    @Bean
    ThresholdEvaluator thresholdEvaluator(MonitoringProperties properties) {
        return new ThresholdEvaluator(properties.thresholds());
    }

    @Bean
    AlarmSink alarmSink() {
        return new LoggingAlarmSink();
    }

    @Bean
    MeasurementSource measurementSource(MonitoringProperties properties, ObjectMapper objectMapper) {
        var kafka = properties.kafka();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, kafka.groupId(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafka.autoOffsetReset(),
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (int) kafka.sessionTimeout().toMillis(),
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (int) kafka.sessionTimeout().toMillis() / 3,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var options = ReceiverOptions.<String, String>create(config).subscription(List.of(kafka.topic()));
        return new KafkaMeasurementSource(options, objectMapper);
    }

    @Bean
    MonitoringService monitoringService(MeasurementSource source, ThresholdEvaluator evaluator, AlarmSink alarmSink) {
        return new MonitoringService(source, evaluator, alarmSink);
    }
}
