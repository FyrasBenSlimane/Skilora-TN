package com.skilora.user.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * CameraDevice - ARM64-compatible camera wrapper using JavaCV.
 * 
 * On macOS: uses FFmpegFrameGrabber with avfoundation (avoids OpenCV's
 * AVFoundation auth threading issue and BridJ x86_64 incompatibility).
 * On other OS: uses OpenCVFrameGrabber.
 */
public class CameraDevice {

    private static final Logger logger = LoggerFactory.getLogger(CameraDevice.class);

    private final FrameGrabber grabber;
    private final Java2DFrameConverter converter;
    private volatile boolean open = false;

    /**
     * Create a CameraDevice for the given device index with specified resolution.
     *
     * @param deviceIndex camera index (0 = default)
     * @param width       requested width (e.g. 640)
     * @param height      requested height (e.g. 480)
     */
    public CameraDevice(int deviceIndex, int width, int height) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            // macOS: use FFmpeg + avfoundation — ARM64 native, no auth threading issue
            FFmpegFrameGrabber fg = new FFmpegFrameGrabber(String.valueOf(deviceIndex));
            fg.setFormat("avfoundation");
            fg.setFrameRate(30);
            this.grabber = fg;
            logger.debug("Using FFmpegFrameGrabber (avfoundation) for macOS");
        } else {
            this.grabber = new OpenCVFrameGrabber(deviceIndex);
            logger.debug("Using OpenCVFrameGrabber for {}", os);
        }
        this.grabber.setImageWidth(width);
        this.grabber.setImageHeight(height);
        this.converter = new Java2DFrameConverter();
    }

    /**
     * Open the camera and start capturing.
     */
    public void open() {
        try {
            grabber.start();
            open = true;
            logger.debug("CameraDevice opened successfully");
        } catch (Exception e) {
            logger.error("Failed to open camera: {}", e.getMessage(), e);
            open = false;
        }
    }

    /**
     * Check if the camera is currently open.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Grab a single frame and return it as a BufferedImage.
     * Skips non-video frames (audio) by checking frame.image.
     *
     * @return captured image, or null if grab failed
     */
    public BufferedImage getImage() {
        if (!open) return null;
        try {
            Frame frame = grabber.grab();
            if (frame == null || frame.image == null) return null;
            return converter.convert(frame);
        } catch (Exception e) {
            logger.error("Error grabbing frame: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Stop capturing and release the camera.
     */
    public void close() {
        if (!open) return;
        try {
            open = false;
            grabber.stop();
            grabber.release();
            logger.debug("CameraDevice closed");
        } catch (Exception e) {
            logger.error("Error closing camera: {}", e.getMessage(), e);
        }
    }
}
