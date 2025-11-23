package com.cpd.transcoder_service.listener;

import com.cpd.transcoder_service.model.VideoReceivedEvent;
import com.cpd.transcoder_service.service.VideoConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class VideoConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoConsumer.class);
    private final VideoConversionService conversionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VideoConsumer(VideoConversionService conversionService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.conversionService = conversionService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "video-watermarked-topic", groupId = "transcoder-group")
    public void consumeVideoEvent(VideoReceivedEvent event) {
        log.info(">>> TRANSCODER: Received task for UUID: {}", event.getUuid());

        File inputFile = new File(event.getFilePath());
        if (!inputFile.exists()) {
            log.error("File not found: {}", event.getFilePath());
            return;
        }

        try {
            String resultPath = conversionService.convertVideo(event.getFilePath(), "final_720p");

            event.setFilePath(resultPath);

            log.info(">>> TRANSCODER: Process complete. Sending completion event.");

            kafkaTemplate.send("video-completed-topic", event);

        } catch (Exception e) {
            log.error("Failed to process video {}", event.getUuid(), e);
        }
    }
}
