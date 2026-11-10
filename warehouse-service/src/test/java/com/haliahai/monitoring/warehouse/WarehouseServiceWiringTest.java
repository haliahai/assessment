package com.haliahai.monitoring.warehouse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.haliahai.monitoring.common.SensorType;
import com.haliahai.monitoring.warehouse.publisher.RecordingMeasurementPublisher;
import com.haliahai.monitoring.warehouse.udp.UdpMeasurementReceiver;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// whole app with real UDP on random ports, Kafka publisher replaced
@SpringBootTest(properties = {
        "warehouse.id=test-warehouse",
        "warehouse.host=127.0.0.1",
        "warehouse.ports.temperature=0",
        "warehouse.ports.humidity=0",
})
class WarehouseServiceWiringTest {

    @Autowired
    UdpMeasurementReceiver receiver;

    @Autowired
    RecordingMeasurementPublisher publisher;

    @Test
    void udpDatagramIsPublishedWithConfiguredWarehouseId() throws Exception {
        var bytes = "sensor_id=h1; value=51\n".getBytes(UTF_8);
        var port = receiver.boundPort(SensorType.HUMIDITY).orElseThrow();
        try (var socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(publisher.published())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.warehouseId()).isEqualTo("test-warehouse");
                    assertThat(m.type()).isEqualTo(SensorType.HUMIDITY);
                    assertThat(m.value()).isEqualByComparingTo("51");
                }));
    }

    @TestConfiguration
    static class RecordingPublisherConfig {

        @Bean
        @Primary
        RecordingMeasurementPublisher recordingMeasurementPublisher() {
            return new RecordingMeasurementPublisher();
        }
    }
}
