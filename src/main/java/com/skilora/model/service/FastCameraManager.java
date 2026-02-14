package com.skilora.model.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fast Camera Manager using webcam-capture library
 * 
 * Opens camera in 1-3 seconds instead of 20+ seconds with OpenCV!
 * Pre-warms camera for instant Face ID.
 */
public class FastCameraManager {
    
    private static final Logger logger = LoggerFactory.getLogger(FastCameraManager.class);
    
    private static FastCameraManager instance;
    private Webcam prewarmedWebcam;
    private AtomicBoolean isPrewarming = new AtomicBoolean(false);
    private AtomicBoolean isReady = new AtomicBoolean(false);
    
    private FastCameraManager() {}
    
    public static synchronized FastCameraManager getInstance() {
        if (instance == null) {
            instance = new FastCameraManager();
        }
        return instance;
    }
    
    /**
     * Start pre-warming camera in background (FAST - 1-3 seconds!)
     */
    public void startPrewarming() {
        if (isPrewarming.get() || isReady.get()) {
            return; // Silently skip if already warming or ready
        }
        
        isPrewarming.set(true);
        logger.debug("Starting FAST camera pre-warm...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Get default webcam (MUCH faster than OpenCV!)
                Webcam cam = Webcam.getDefault();
                
                if (cam == null) {
                    logger.error("No webcam found!");
                    isPrewarming.set(false);
                    return;
                }
                
                // Set resolution (VGA = 640x480, better quality than QVGA)
                cam.setViewSize(WebcamResolution.VGA.getSize()); 
                
                logger.debug("Opening camera...");
                cam.open();
                
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("Camera READY in {}ms (FAST!)", duration);
                
                prewarmedWebcam = cam;
                isReady.set(true);
                isPrewarming.set(false);
                
            } catch (Exception e) {
                // Silently ignore errors in background pre-warming (doesn't affect main flow)
                isPrewarming.set(false);
                prewarmedWebcam = null;
            }
        });
    }
    
    /**
     * Get pre-warmed camera (INSTANT if ready!)
     */
    public Webcam getPrewarmedCamera() {
        if (isReady.get() && prewarmedWebcam != null) {
            logger.debug("Returning INSTANT pre-warmed camera!");
            Webcam camera = prewarmedWebcam;
            prewarmedWebcam = null;
            isReady.set(false);
            
            // Start pre-warming next camera in background
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Small delay before re-warming
                    resetAndPrewarm();
                } catch (Exception e) {
                    // Ignore
                }
            });
            
            return camera;
        }
        return null;
    }
    
    /**
     * Get camera immediately (will be fast even if not pre-warmed)
     */
    public Webcam getCameraFast() {
        try {
            Webcam cam = Webcam.getDefault();
            if (cam != null) {
                // Set resolution to VGA (640x480) for better quality
                cam.setViewSize(WebcamResolution.VGA.getSize());
                if (!cam.isOpen()) {
                    long start = System.currentTimeMillis();
                    cam.open();
                    logger.debug("Camera opened in {}ms", (System.currentTimeMillis() - start));
                }
            }
            return cam;
        } catch (Exception e) {
            logger.error("Error getting camera: " + e.getMessage(), e);
            return null;
        }
    }
    
    public boolean isReady() {
        return isReady.get();
    }
    
    public boolean isWarming() {
        return isPrewarming.get();
    }
    
    /**
     * Reset and start fresh pre-warming
     */
    public void resetAndPrewarm() {
        isReady.set(false);
        isPrewarming.set(false);
        if (prewarmedWebcam != null) {
            try {
                if (prewarmedWebcam.isOpen()) {
                    prewarmedWebcam.close();
                }
            } catch (Exception e) {
                // Ignore
            }
            prewarmedWebcam = null;
        }
        startPrewarming();
    }
    
    /**
     * Close a webcam safely
     */
    public void closeCamera(Webcam webcam) {
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Exception e) {
                logger.error("Error closing camera: " + e.getMessage(), e);
            }
        }
    }
}
