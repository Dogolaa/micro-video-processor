package com.cpd.resizer_service.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Service
public class ResizerProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ResizerProcessingService.class);

    public String resizeVideo(String inputPath) {
        // Define saída (ex: video_resized.mp4)
        String outputPath = inputPath.replace(".mp4", "_resized.mp4");

        // Target: 480p Wide
        int width = 854;
        int height = 480;

        log.info("Starting resize: {} -> {} ({}x{})", inputPath, outputPath, width, height);
        long startTime = System.currentTimeMillis();

        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        // Conversor para manipular imagem com OpenCV
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        try {
            grabber = new FFmpegFrameGrabber(inputPath);
            grabber.start();

            recorder = new FFmpegFrameRecorder(outputPath, width, height, grabber.getAudioChannels());
            recorder.setFormat("mp4");
            recorder.setFrameRate(grabber.getFrameRate());

            // Configurações Seguras (H.264) para evitar erros
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "23");

            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setSampleRate(grabber.getSampleRate());
            }

            recorder.start();

            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    // Lógica de Resize usando OpenCV
                    Mat mat = converter.convert(frame);
                    Mat resizedMat = new Mat();

                    // Redimensiona a imagem
                    resize(mat, resizedMat, new Size(width, height));

                    // Converte de volta para Frame e grava
                    Frame resizedFrame = converter.convert(resizedMat);
                    recorder.record(resizedFrame);

                    // Libera memória nativa (Importante no JavaCV!)
                    mat.release();
                    resizedMat.release();
                } else {
                    // Se for áudio, apenas grava
                    recorder.record(frame);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Resize finished in {} ms. File: {}", duration, outputPath);

            return outputPath;

        } catch (Exception e) {
            log.error("Error resizing video", e);
            throw new RuntimeException("Resize failed", e);
        } finally {
            try { if (recorder != null) recorder.close(); } catch (Exception e) {}
            try { if (grabber != null) grabber.close(); } catch (Exception e) {}
        }
    }
}