package com.skilora.user.service;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fast Camera Manager using JavaCV (OpenCVFrameGrabber) — ARM64 compatible.
 * 
 * Replaces sarxos webcam-capture which only supported x86_64 via BridJ.
 * Pre-warms camera for instant Face ID access.
 */
public class FastCameraManager {
    
    private static final Logger logger = LoggerFactory.getLogger(FastCameraManager.class);
    
    private static final int CAM_WIDTH = 640;  // VGA
    private static final int CAM_HEIGHT = 480;
    
    private static FastCameraManager instance;
    private CameraDevice prewarmedCamera;
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
     * Start pre-warming camera in background.
     */
    public void startPrewarming() {
        if (isPrewarming.get() || isReady.get()) {
            return;
        }
        
        isPrewarming.set(true);
        logger.debug("Starting camera pre-warm (JavaCV)...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                CameraDevice cam = new CameraDevice(0, CAM_WIDTH, CAM_HEIGHT);
                cam.open();
                
                if (!cam.isOpen()) {
                    logger.error("Failed to open camera during pre-warm");
                    isPrewarming.set(false);
                    return;
                }
                
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("Camera READY in {}ms", duration);
                
                prewarmedCamera = cam;
                isReady.set(true);
                isPrewarming.set(false);
                
            } catch (Exception e) {
                logger.debug("Camera pre-warm failed: {}", e.getMessage());
                isPrewarming.set(false);
                prewarmedCamera = null;
            }
        });
    }
    
    /**
     * Get pre-warmed camera (INSTANT if ready!)
     */
    public CameraDevice getPrewarmedCamera() {
        if (isReady.get() && prewarmedCamera != null) {
            logger.debug("Returning pre-warmed camera!");
            CameraDevice camera = prewarmedCamera;
            prewarmedCamera = null;
            isReady.set(false);
            
            // Start pre-warming next camera in background
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500);
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
     * Get camera immediately (will open a new one if not pre-warmed)
     */
    public CameraDevice getCameraFast() {
        try {
            CameraDevice cam = new CameraDevice(0, CAM_WIDTH, CAM_HEIGHT);
            long start = System.currentTimeMillis();
            cam.open();
            logger.debug("Camera opened in {}ms", (System.currentTimeMillis() - start));
            
            if (!cam.isOpen()) {
                logger.error("Camera failed to open");
                return null;
            }
            return cam;
        } catch (Exception e) {
            logger.error("Error getting camera: {}", e.getMessage(), e);
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
        if (prewarmedCamera != null) {
            try {
                prewarmedCamera.close();
            } catch (Exception e) {
                // Ignore
            }
            prewarmedCamera = null;
        }
        startPrewarming();
    }
    
    /**
     * Close a camera safely
     */
    public void closeCamera(CameraDevice camera) {
        if (camera != null) {
            try {
                camera.close();
            } catch (Exception e) {
                logger.error("Error closing camera: {}", e.getMessage(), e);
            }
        }
    }
}
