package com.cpd.upload_api.controller;

import com.cpd.upload_api.model.VideoReceivedEvent;
import com.cpd.upload_api.service.VideoProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final Path storagePath = Paths.get("/app/storage/videos");

    private final VideoProducerService producerService;

    public UploadController(VideoProducerService producerService) {
        this.producerService = producerService;
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de armazenamento", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Object> uploadVideos(@RequestParam("files") List<MultipartFile> files) {

        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body("A lista de arquivos está vazia");
        }

        log.info("Recebido lote de {} arquivos para upload.", files.size());
        List<String> uploadedUuids = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            try {
                String uuid = UUID.randomUUID().toString();

                // Define o caminho final
                Path destination = storagePath.resolve(uuid + "_" + originalFilename);

                // Salva no disco (Volume Compartilhado)
                Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                log.info("Vídeo salvo: {}", destination);

                // Cria o evento
                VideoReceivedEvent event = new VideoReceivedEvent(uuid, originalFilename, destination.toString());

                producerService.sendVideoReceivedEvent(event);

                uploadedUuids.add(uuid);

            } catch (IOException e) {
                log.error("Erro ao processar o arquivo: " + originalFilename, e);
                errors.add("Erro em " + originalFilename + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok().body(new UploadResponse(
                "Upload de lote concluído",
                uploadedUuids.size(),
                uploadedUuids,
                errors
        ));
    }

    record UploadResponse(String message, int count, List<String> successIds, List<String> errors) {}
}
