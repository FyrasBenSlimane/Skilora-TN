package tn.esprit.skylora.utils;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioRecorder {
    private TargetDataLine line;
    private boolean isRecording = false;
    private ByteArrayOutputStream out;

    public void start() throws LineUnavailableException {
        // Change to little-endian (false) for standard WAV compatibility
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Line not supported");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        isRecording = true;
        out = new ByteArrayOutputStream();

        Thread thread = new Thread(() -> {
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
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public byte[] stop() {
        isRecording = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        byte[] rawData = out.toByteArray();
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(rawData);
            AudioInputStream ais = new AudioInputStream(bais, format, rawData.length / format.getFrameSize());
            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return rawData;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}
