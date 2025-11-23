package com.cpd.watermarker_service.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class WatermarkService {

    private static final Logger log = LoggerFactory.getLogger(WatermarkService.class);

    public String addWatermark(String inputPath) {
        String outputPath = inputPath.replace(".mp4", "_wmark.mp4");
        log.info("Applying watermark: {} -> {}", inputPath, outputPath);
        long startTime = System.currentTimeMillis();

        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        try {
            grabber = new FFmpegFrameGrabber(inputPath);
            grabber.start();

            recorder = new FFmpegFrameRecorder(outputPath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder.setFormat("mp4");
            recorder.setFrameRate(grabber.getFrameRate());

            // Configurações de Codec (H.264)
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "23");

            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setSampleRate(grabber.getSampleRate());
            }

            recorder.start();

            // Configura a cor e a fonte da marca d'água
            Scalar color = new Scalar(255, 255, 255, 0); // Branco
            int font = FONT_HERSHEY_SIMPLEX;

            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    Mat mat = converter.convert(frame);
                    // Escreve no vídeo
                    putText(mat, "CPD - MICROSERVICE", new Point(50, 50), font, 1.5, color, 3, LINE_AA, false);

                    Frame wFrame = converter.convert(mat);
                    recorder.record(wFrame);

                    // Libera memória (crucial)
                    mat.release();
                } else {
                    recorder.record(frame); // Áudio
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Watermark finished in {} ms.", duration);
            return outputPath;

        } catch (Exception e) {
            log.error("Error adding watermark", e);
            throw new RuntimeException("Watermark failed", e);
        } finally {
            try { if (recorder != null) recorder.close(); } catch (Exception e) {}
            try { if (grabber != null) grabber.close(); } catch (Exception e) {}
        }
    }
}
