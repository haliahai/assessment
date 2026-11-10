package com.haliahai.monitoring.warehouse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haliahai.monitoring.warehouse.parser.MeasurementParser;
import com.haliahai.monitoring.warehouse.pipeline.MeasurementPipeline;
import com.haliahai.monitoring.warehouse.publisher.KafkaMeasurementPublisher;
import com.haliahai.monitoring.warehouse.publisher.MeasurementPublisher;
import com.haliahai.monitoring.warehouse.udp.UdpMeasurementReceiver;
import java.time.Clock;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Configuration(proxyBeanMethods = false)
class WarehouseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    MeasurementParser measurementParser() {
        return new MeasurementParser();
    }

    @Bean(destroyMethod = "close")
    KafkaSender<String, String> kafkaSender(WarehouseProperties properties) {
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.kafka().bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.CLIENT_ID_CONFIG, "warehouse-" + properties.id());
        return KafkaSender.create(SenderOptions.create(config));
    }

    @Bean
    MeasurementPublisher measurementPublisher(
            KafkaSender<String, String> kafkaSender, ObjectMapper objectMapper, WarehouseProperties properties) {
        return new KafkaMeasurementPublisher(kafkaSender, objectMapper, properties.kafka().topic());
    }

    @Bean
    MeasurementPipeline measurementPipeline(
            WarehouseProperties properties, MeasurementParser parser, MeasurementPublisher publisher, Clock clock) {
        return new MeasurementPipeline(
                properties.id(), parser, publisher, clock, properties.bufferSize(), properties.maxInFlight());
    }

    @Bean
    UdpMeasurementReceiver udpMeasurementReceiver(WarehouseProperties properties, MeasurementPipeline pipeline) {
        return new UdpMeasurementReceiver(properties.host(), properties.ports(), pipeline);
    }
}
