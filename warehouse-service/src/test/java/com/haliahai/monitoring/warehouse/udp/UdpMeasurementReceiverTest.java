package com.haliahai.monitoring.warehouse.udp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.haliahai.monitoring.common.Measurement;
import com.haliahai.monitoring.common.SensorType;
import com.haliahai.monitoring.warehouse.parser.MeasurementParser;
import com.haliahai.monitoring.warehouse.pipeline.MeasurementPipeline;
import com.haliahai.monitoring.warehouse.publisher.RecordingMeasurementPublisher;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UdpMeasurementReceiverTest {

    private final RecordingMeasurementPublisher publisher = new RecordingMeasurementPublisher();
    private UdpMeasurementReceiver receiver;

    @BeforeEach
    void startReceiverOnEphemeralPorts() {
        var pipeline = new MeasurementPipeline("w1", new MeasurementParser(), publisher, Clock.systemUTC(), 16, 4);
        receiver = new UdpMeasurementReceiver(
                "127.0.0.1", Map.of(SensorType.TEMPERATURE, 0, SensorType.HUMIDITY, 0), pipeline);
        receiver.start();
    }

    @AfterEach
    void stopReceiver() {
        receiver.stop();
    }

    @Test
    void datagramsOnEachPortArriveWithTheMatchingType() throws IOException {
        send(SensorType.TEMPERATURE, "sensor_id=t1; value=30\n");
        send(SensorType.HUMIDITY, "sensor_id=h1; value=40\n");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(publisher.published())
                .extracting(Measurement::sensorId, Measurement::type)
                .containsExactlyInAnyOrder(
                        tuple("t1", SensorType.TEMPERATURE),
                        tuple("h1", SensorType.HUMIDITY)));
    }

    private void send(SensorType type, String payload) throws IOException {
        var port = receiver.boundPort(type).orElseThrow();
        var bytes = payload.getBytes(UTF_8);
        try (var socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));
        }
    }
}
