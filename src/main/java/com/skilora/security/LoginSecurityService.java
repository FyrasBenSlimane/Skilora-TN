package com.skilora.security;

import com.skilora.community.entity.Notification;
import com.skilora.community.service.EmailService;
import com.skilora.community.service.NotificationService;
import com.skilora.user.service.FastCameraManager;
import com.skilora.user.entity.User;
import com.skilora.user.service.UserService;
import com.skilora.utils.AppThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * LoginSecurityService — orchestrates security responses to failed login attempts:
 * <ul>
 *   <li>Camera capture after N failed attempts (configurable threshold)</li>
 *   <li>Admin notification (in-app + email)</li>
 *   <li>Security alert persistence</li>
 * </ul>
 */
public class LoginSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(LoginSecurityService.class);
    private static volatile LoginSecurityService instance;

    /** Number of failed attempts before triggering camera capture */
    private static final int CAPTURE_THRESHOLD = 3;

    /** Directory where captured photos are saved */
    private static final String CAPTURE_DIR = "security_captures";

    private LoginSecurityService() {
        // Ensure capture directory exists
        new File(CAPTURE_DIR).mkdirs();
    }

    public static LoginSecurityService getInstance() {
        if (instance == null) {
            synchronized (LoginSecurityService.class) {
                if (instance == null) {
                    instance = new LoginSecurityService();
                }
            }
        }
        return instance;
    }

    /**
     * Called after each failed login attempt. Checks if the threshold is reached
     * and triggers camera capture + admin notification asynchronously.
     *
     * @param username       the username that failed login
     * @param failedCount    total number of recent failed attempts for this user
     * @param isNowLockedOut whether the account just became locked
     */
    public void onLoginFailed(String username, int failedCount, boolean isNowLockedOut) {
        if (failedCount < CAPTURE_THRESHOLD) {
            return; // Not enough attempts to trigger yet
        }

        // Only capture on the exact threshold or on lockout
        if (failedCount == CAPTURE_THRESHOLD || isNowLockedOut) {
            AppThreadPool.execute(() -> {
                try {
                    String photoPath = capturePhoto(username);
                    String ipAddress = getLocalIpAddress();

                    // Create security alert record
                    SecurityAlert alert = new SecurityAlert(username,
                            isNowLockedOut ? "LOCKOUT" : "FAILED_LOGIN_CAPTURE",
                            failedCount);
                    alert.setPhotoPath(photoPath);
                    alert.setIpAddress(ipAddress);
                    int alertId = SecurityAlertService.getInstance().create(alert);

                    // Notify all admins
                    notifyAdmins(username, failedCount, photoPath, isNowLockedOut, alertId);

                    logger.info("Security capture completed for '{}': photo={}, alertId={}",
                            username, photoPath != null ? "captured" : "failed", alertId);
                } catch (Exception e) {
                    logger.error("Security capture failed for '{}': {}", username, e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Attempt to capture a photo from the webcam.
     * Returns the file path of the saved image, or null if capture failed.
     */
    private String capturePhoto(String username) {
        try {
            FastCameraManager camManager = FastCameraManager.getInstance();
            com.skilora.user.service.CameraDevice camera = camManager.getCameraFast();

            if (camera == null || !camera.isOpen()) {
                logger.warn("Camera not available for security capture");
                return null;
            }

            // Grab a frame as BufferedImage via CameraDevice wrapper
            BufferedImage image = camera.getImage();

            // Release camera immediately
            camera.close();

            if (image == null) {
                logger.warn("Camera returned null image");
                return null;
            }

            // Save to file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = String.format("%s/capture_%s_%s.png", CAPTURE_DIR, sanitizedUser, timestamp);

            File outputFile = new File(filename);
            ImageIO.write(image, "png", outputFile);

            logger.info("Security photo captured: {}", filename);
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("Camera capture error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Notify all admin users about the security event — both in-app notification and email.
     */
    private void notifyAdmins(String username, int failedAttempts, String photoPath,
                              boolean isLockedOut, int alertId) {
        try {
            // Find all admin users
            List<User> admins = UserService.getInstance().findByRole("ADMIN");

            String title = isLockedOut
                    ? "🔒 Account Locked: " + username
                    : "⚠️ Suspicious Login: " + username;
            String message = String.format(
                    "%d failed login attempts for '%s'.%s%s",
                    failedAttempts,
                    username,
                    isLockedOut ? " Account has been locked out." : "",
                    photoPath != null ? " Camera capture saved." : " Camera unavailable."
            );

            // In-app notification to each admin
            for (User admin : admins) {
                Notification notif = new Notification(admin.getId(), "SECURITY", title, message);
                notif.setIcon("🛡️");
                notif.setReferenceType("security_alert");
                notif.setReferenceId(alertId > 0 ? alertId : null);
                NotificationService.getInstance().create(notif);
            }

            // Email notification to first admin (or all if desired)
            if (!admins.isEmpty()) {
                User primaryAdmin = admins.get(0);
                if (primaryAdmin.getEmail() != null && !primaryAdmin.getEmail().isBlank()) {
                    sendSecurityEmail(primaryAdmin.getEmail(), username, failedAttempts,
                            photoPath, isLockedOut);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to notify admins about security event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a security alert email to an admin.
     */
    private void sendSecurityEmail(String adminEmail, String username, int failedAttempts,
                                   String photoPath, boolean isLockedOut) {
        try {
            EmailService.getInstance().sendSecurityAlertEmail(
                    adminEmail, username, failedAttempts, photoPath, isLockedOut);
        } catch (Exception e) {
            logger.error("Failed to send security alert email: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the local machine IP address (best effort).
     */
    private String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
