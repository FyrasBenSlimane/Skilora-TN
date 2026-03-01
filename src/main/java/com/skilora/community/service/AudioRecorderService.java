package com.skilora.community.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service d'enregistrement vocal — Capture le micro et sauvegarde en WAV.
 * Utilise javax.sound.sampled (inclus dans le JDK, pas de dépendance externe).
 *
 * Usage typique :
 *   AudioRecorderService recorder = AudioRecorderService.getInstance();
 *   recorder.startRecording();
 *   // ... l'utilisateur parle ...
 *   File wavFile = recorder.stopRecording();  // retourne le fichier WAV
 */
public class AudioRecorderService {

    private static final Logger logger = LoggerFactory.getLogger(AudioRecorderService.class);

    // Format audio : PCM 16-bit, 44.1 kHz, mono
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,   // sample rate
            16,      // bits par échantillon
            1,       // mono
            2,       // frame size (16 bits = 2 bytes)
            44100,   // frame rate
            false    // little-endian
    );

    private static final String RECORDINGS_DIR = "data/uploads/vocal";

    // ── Singleton ──
    private static volatile AudioRecorderService instance;
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

    // ── État ──
    private volatile boolean recording = false;
    private TargetDataLine targetLine;
    private Thread recordingThread;
    private File currentFile;
    private long recordingStartTime;

    /**
     * Démarre l'enregistrement vocal depuis le microphone par défaut.
     * L'enregistrement tourne dans un thread séparé.
     */
    public void startRecording() {
        if (recording) {
            logger.warn("Recording already in progress — ignoring start request");
            return;
        }

        try {
            // Préparer le dossier de sortie
            Path dir = Paths.get(RECORDINGS_DIR);
            Files.createDirectories(dir);

            // Nom de fichier unique basé sur le timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            currentFile = dir.resolve("vocal_" + timestamp + ".wav").toFile();

            // Ouvrir la ligne micro
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("Le microphone n'est pas disponible ou le format audio n'est pas supporté.");
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(AUDIO_FORMAT);
            targetLine.start();
            recording = true;
            recordingStartTime = System.currentTimeMillis();

            // Thread d'enregistrement — écrit directement en WAV
            recordingThread = new Thread(() -> {
                try {
                    AudioInputStream audioStream = new AudioInputStream(targetLine);
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, currentFile);
                } catch (IOException e) {
                    if (recording) {
                        logger.error("Erreur pendant l'enregistrement : {}", e.getMessage());
                    }
                }
            }, "AudioRecorderThread");
            recordingThread.setDaemon(true);
            recordingThread.start();

            logger.info("🎙 Recording started → {}", currentFile.getName());

        } catch (LineUnavailableException e) {
            recording = false;
            logger.error("Impossible d'ouvrir le micro : {}", e.getMessage());
            throw new RuntimeException("Microphone non disponible : " + e.getMessage());
        } catch (IOException e) {
            recording = false;
            logger.error("Erreur IO lors du démarrage : {}", e.getMessage());
            throw new RuntimeException("Erreur d'enregistrement : " + e.getMessage());
        }
    }

    /**
     * Arrête l'enregistrement et retourne le fichier WAV.
     * @return Le fichier WAV enregistré, ou null si aucun enregistrement en cours.
     */
    public File stopRecording() {
        if (!recording || targetLine == null) {
            logger.warn("No recording in progress — ignoring stop request");
            return null;
        }

        recording = false;

        // Arrêter la ligne (déclenche la fin de AudioSystem.write)
        targetLine.stop();
        targetLine.close();

        // Attendre la fin du thread d'écriture
        try {
            if (recordingThread != null) {
                recordingThread.join(3000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long durationMs = System.currentTimeMillis() - recordingStartTime;
        logger.info("🎙 Recording stopped — {} ({} ms)", currentFile.getName(), durationMs);

        // Vérifier que le fichier est valide (au moins 1 seconde)
        if (durationMs < 800) {
            logger.warn("Recording too short ({} ms) — discarding", durationMs);
            if (currentFile.exists()) currentFile.delete();
            return null;
        }

        return currentFile;
    }

    /**
     * Annule l'enregistrement en cours et supprime le fichier.
     */
    public void cancelRecording() {
        if (!recording || targetLine == null) return;

        recording = false;
        targetLine.stop();
        targetLine.close();

        try {
            if (recordingThread != null) recordingThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (currentFile != null && currentFile.exists()) {
            currentFile.delete();
            logger.info("Recording cancelled and file deleted");
        }
    }

    /** @return true si un enregistrement est en cours */
    public boolean isRecording() {
        return recording;
    }

    /** @return Durée en secondes de l'enregistrement en cours */
    public int getElapsedSeconds() {
        if (!recording) return 0;
        return (int) ((System.currentTimeMillis() - recordingStartTime) / 1000);
    }

    /** @return Durée d'un fichier WAV en secondes */
    public static int getWavDurationSeconds(File wavFile) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
            AudioFormat format = ais.getFormat();
            long frames = ais.getFrameLength();
            double durationSec = (frames / format.getFrameRate());
            ais.close();
            return (int) Math.ceil(durationSec);
        } catch (Exception e) {
            // Fallback : estimer depuis la taille du fichier
            // WAV PCM 16-bit mono 44100 Hz ≈ 88200 bytes/sec + 44 bytes header
            long dataSize = wavFile.length() - 44;
            return (int) Math.max(1, dataSize / 88200);
        }
    }

    /** Formate une durée en secondes → "0:12", "1:05", etc. */
    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
