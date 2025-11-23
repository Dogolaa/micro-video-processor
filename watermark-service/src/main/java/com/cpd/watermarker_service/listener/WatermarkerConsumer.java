package com.cpd.watermarker_service.listener;

import com.cpd.watermarker_service.model.VideoReceivedEvent;
import com.cpd.watermarker_service.service.WatermarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WatermarkerConsumer {

    private static final Logger log = LoggerFactory.getLogger(WatermarkerConsumer.class);
    private final WatermarkService watermarkService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WatermarkerConsumer(WatermarkService watermarkService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.watermarkService = watermarkService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "video-resized-topic", groupId = "watermarker-group")
    public void consume(VideoReceivedEvent event) {
        log.info(">>> WATERMARKER: Received task for UUID: {}", event.getUuid());

        File inputFile = new File(event.getFilePath());
        if (!inputFile.exists()) {
            log.error("File not found: {}", event.getFilePath());
            return;
        }

        try {
            // 1. Aplica a marca d'água
            String newPath = watermarkService.addWatermark(event.getFilePath());

            // 2. Atualiza o caminho
            event.setFilePath(newPath);

            // 3. Manda para o Transcoder (última etapa)
            log.info("Sending to Transcoder topic (video-watermarked-topic)...");
            kafkaTemplate.send("video-watermarked-topic", event);

        } catch (Exception e) {
            log.error("Processing failed", e);
        }
    }
}
