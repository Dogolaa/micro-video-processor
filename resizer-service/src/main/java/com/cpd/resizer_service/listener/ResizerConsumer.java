package com.cpd.resizer_service.listener;

import com.cpd.resizer_service.model.VideoReceivedEvent;
import com.cpd.resizer_service.service.ResizerProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ResizerConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResizerConsumer.class);
    private final ResizerProcessingService resizerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ResizerConsumer(ResizerProcessingService resizerService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.resizerService = resizerService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // 1. Escuta o tópico de entrada (vindo do Upload)
    @KafkaListener(topics = "video-received-topic", groupId = "resizer-group")
    public void consume(VideoReceivedEvent event) {
        log.info(">>> RESIZER: Received task for UUID: {}", event.getUuid());

        File inputFile = new File(event.getFilePath());
        if (!inputFile.exists()) {
            log.error("File not found: {}", event.getFilePath());
            return;
        }

        try {
            // 2. Processa (Redimensiona)
            String newPath = resizerService.resizeVideo(event.getFilePath());

            event.setFilePath(newPath);

            // 3. Envia para o próximo estágio (Watermarker)
            // Tópico de saída: 'video-resized-topic'
            log.info("Sending to Watermarker topic (video-resized-topic)...");
            kafkaTemplate.send("video-resized-topic", event);

        } catch (Exception e) {
            log.error("Processing failed", e);
        }
    }
}
