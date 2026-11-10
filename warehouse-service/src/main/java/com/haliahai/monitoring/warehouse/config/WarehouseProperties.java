package com.haliahai.monitoring.warehouse.config;

import com.haliahai.monitoring.common.SensorType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("warehouse")
public record WarehouseProperties(
        String id,
        String host,
        Map<SensorType, Integer> ports,
        int bufferSize,
        int maxInFlight,
        Kafka kafka) {

    public WarehouseProperties {
        requireNonBlank(id, "warehouse.id");
        requireNonBlank(host, "warehouse.host");
        Objects.requireNonNull(ports, "warehouse.ports");
        if (ports.isEmpty()) {
            throw new IllegalArgumentException("warehouse.ports must configure at least one sensor type");
        }
        ports.forEach((type, port) -> {
            if (port == null || port < 0 || port > 65_535) {
                throw new IllegalArgumentException("warehouse.ports." + type + " is not a valid port: " + port);
            }
        });
        requirePositive(bufferSize, "warehouse.buffer-size");
        requirePositive(maxInFlight, "warehouse.max-in-flight");
        Objects.requireNonNull(kafka, "warehouse.kafka");
        ports = Map.copyOf(new EnumMap<>(ports));
    }

    public record Kafka(String bootstrapServers, String topic) {

        public Kafka {
            requireNonBlank(bootstrapServers, "warehouse.kafka.bootstrap-servers");
            requireNonBlank(topic, "warehouse.kafka.topic");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be set");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
    }
}
