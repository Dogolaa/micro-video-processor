package com.cpd.upload_api.service;

import com.cpd.upload_api.model.VideoReceivedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VideoProducerService {

    private static final Logger log = LoggerFactory.getLogger(VideoProducerService.class);
    private static final String TOPIC = "video-received-topic";

    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VideoProducerService() {
        log.info(">>> INICIANDO PRODUCER MANUALMENTE PARA: {}", BOOTSTRAP_SERVERS);

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
        this.kafkaTemplate = new KafkaTemplate<>(factory);
    }

    public void sendVideoReceivedEvent(VideoReceivedEvent event) {
        log.info("Sending event to Kafka topic '{}': {}", TOPIC, event.getOriginalFilename());
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Event sent successfully!");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send message to Kafka", e);
            throw e;
        }
    }
}