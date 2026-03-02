package com.skilora.support.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for recording audio from the system microphone.
 * Captures audio in WAV format suitable for speech-to-text transcription.
 *
 * Singleton — obtain via {@link #getInstance()}.
 */
public class AudioRecorderService {

    private static final Logger logger = LoggerFactory.getLogger(AudioRecorderService.class);
    private static volatile AudioRecorderService instance;

    private TargetDataLine line;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream out;

    // ── Singleton ──

    private AudioRecorderService() {}

    public static AudioRecorderService getInstance() {
        if (instance == null) {
            synchronized (AudioRecorderService.class) {
                if (instance == null) {
                    instance = new AudioRecorderService();
                }
            }
        }
        return instance;
    }

    // ── Public API ──

    /**
     * Starts recording audio from the default microphone.
     * Recording happens in a background thread.
     *
     * @throws LineUnavailableException if the audio line is not available
     */
    public void start() throws LineUnavailableException {
        // Standard WAV format: 44.1kHz, 16-bit, mono, signed, little-endian
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio line not supported");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        isRecording = true;
        out = new ByteArrayOutputStream();

        Thread captureThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRecording) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            try {
                out.close();
            } catch (IOException e) {
                logger.error("Error closing audio output stream", e);
            }
        }, "AudioRecorderService-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stops recording and returns the captured audio as a WAV byte array.
     *
     * @return WAV-encoded byte array of the recorded audio, or raw PCM data on conversion error
     */
    public byte[] stop() {
        isRecording = false;
        if (line != null) {
            line.stop();
            line.close();
        }

        byte[] rawData = out.toByteArray();
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
            AudioInputStream ais = new AudioInputStream(bais, format, rawData.length / format.getFrameSize());
            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        } catch (Exception e) {
            logger.error("Error converting audio to WAV format", e);
            return rawData;
        }
    }

    /**
     * @return true if currently recording audio
     */
    public boolean isRecording() {
        return isRecording;
    }
}
