package com.haliahai.monitoring.warehouse.udp;

import com.haliahai.monitoring.common.SensorType;
import com.haliahai.monitoring.warehouse.pipeline.MeasurementPipeline;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

public final class UdpMeasurementReceiver implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(UdpMeasurementReceiver.class);
    private static final Duration BIND_TIMEOUT = Duration.ofSeconds(10);

    private final String host;
    private final Map<SensorType, Integer> ports;
    private final MeasurementPipeline pipeline;
    private volatile Map<SensorType, Connection> connections = Map.of();

    public UdpMeasurementReceiver(String host, Map<SensorType, Integer> ports, MeasurementPipeline pipeline) {
        this.host = Objects.requireNonNull(host, "host");
        this.ports = Map.copyOf(ports);
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    @Override
    public void start() {
        var bound = new EnumMap<SensorType, Connection>(SensorType.class);
        try {
            ports.forEach((type, port) -> bound.put(type, bind(type, port)));
        } catch (RuntimeException e) {
            bound.values().forEach(Connection::disposeNow);
            throw e;
        }
        connections = Map.copyOf(bound);
    }

    private Connection bind(SensorType type, int port) {
        var connection = UdpServer.create()
                .host(host)
                .port(port)
                .handle((in, out) -> pipeline
                        .process(type, in.receiveObject()
                                .ofType(DatagramPacket.class)
                                // copy: Netty releases the buffer after this
                                .map(packet -> ByteBufUtil.getBytes(packet.content())))
                        .then())
                .bindNow(BIND_TIMEOUT);
        log.info("Listening for {} measurements on udp/{}", type, boundPort(connection));
        return connection;
    }

    @Override
    public void stop() {
        connections.values().forEach(Connection::disposeNow);
        connections = Map.of();
    }

    @Override
    public boolean isRunning() {
        return !connections.isEmpty();
    }

    // real port when configured as 0 (tests)
    public Optional<Integer> boundPort(SensorType type) {
        return Optional.ofNullable(connections.get(type)).map(UdpMeasurementReceiver::boundPort);
    }

    private static int boundPort(Connection connection) {
        return ((InetSocketAddress) connection.address()).getPort();
    }
}
