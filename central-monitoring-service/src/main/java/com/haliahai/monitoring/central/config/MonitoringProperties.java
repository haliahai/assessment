package com.haliahai.monitoring.central.config;

import com.haliahai.monitoring.common.SensorType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("monitoring")
public record MonitoringProperties(Map<SensorType, BigDecimal> thresholds, Kafka kafka) {

    public MonitoringProperties {
        Objects.requireNonNull(thresholds, "monitoring.thresholds");
        Objects.requireNonNull(kafka, "monitoring.kafka");
        thresholds = Map.copyOf(thresholds);
    }

    public record Kafka(
            String bootstrapServers, String topic, String groupId, String autoOffsetReset, Duration sessionTimeout) {

        public Kafka {
            requireNonBlank(bootstrapServers, "monitoring.kafka.bootstrap-servers");
            requireNonBlank(topic, "monitoring.kafka.topic");
            requireNonBlank(groupId, "monitoring.kafka.group-id");
            requireNonBlank(autoOffsetReset, "monitoring.kafka.auto-offset-reset");
            Objects.requireNonNull(sessionTimeout, "monitoring.kafka.session-timeout");
        }

        private static void requireNonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must be set");
            }
        }
    }
}
