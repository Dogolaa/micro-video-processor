package com.cpd.transcoder_service.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VideoConversionService {

    private static final Logger log = LoggerFactory.getLogger(VideoConversionService.class);

    /**
     * Converte o vídeo aplicando um sufixo ao nome do arquivo.
     * Ex: input.mp4 -> input_final_720p.mp4
     */
    public String convertVideo(String inputPath, String outputSuffix) {
        // Constrói o caminho de saída adicionando o sufixo
        String outputPath = inputPath.replace(".mp4", "_" + outputSuffix + ".mp4");

        log.info("Starting conversion: {} -> {}", inputPath, outputPath);
        long startTime = System.currentTimeMillis();

        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;

        try {
            // 1. Configura o Leitor (Grabber)
            grabber = new FFmpegFrameGrabber(inputPath);
            grabber.start();

            // 2. Configura o Escritor (Recorder)
            recorder = new FFmpegFrameRecorder(outputPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder.setFormat("mp4");
            recorder.setFrameRate(grabber.getFrameRate());

            // Configurações vitais de Codec (H.264) para evitar erros
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setVideoOption("crf", "23");       // Qualidade
            recorder.setVideoOption("preset", "ultrafast"); // Velocidade

            // Configura áudio se existir
            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            }

            recorder.start();

            // 3. Processa frame a frame
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Conversion finished in {} ms. File created: {}", duration, outputPath);

            return outputPath;

        } catch (Exception e) {
            log.error("Error converting video", e);
            throw new RuntimeException("Conversion failed", e);
        } finally {
            // Limpeza de recursos segura
            try { if (recorder != null) recorder.close(); } catch (Exception e) { log.warn("Error closing recorder", e); }
            try { if (grabber != null) grabber.close(); } catch (Exception e) { log.warn("Error closing grabber", e); }
        }
    }
}