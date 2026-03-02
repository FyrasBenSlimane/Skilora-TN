package com.skilora.formation.service;

import com.skilora.formation.entity.Achievement;
import com.skilora.formation.entity.Certificate;
import com.skilora.formation.entity.Enrollment;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.enums.BadgeRarity;
import com.skilora.formation.enums.EnrollmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public class CertificateGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateGenerationService.class);
    private static volatile CertificateGenerationService instance;

    private final CertificateService certificateService;
    private final EnrollmentService enrollmentService;
    private final FormationService formationService;
    private final AchievementService achievementService;

    private CertificateGenerationService() {
        this.certificateService = CertificateService.getInstance();
        this.enrollmentService = EnrollmentService.getInstance();
        this.formationService = FormationService.getInstance();
        this.achievementService = AchievementService.getInstance();
    }

    public static CertificateGenerationService getInstance() {
        if (instance == null) {
            synchronized (CertificateGenerationService.class) {
                if (instance == null) {
                    instance = new CertificateGenerationService();
                }
            }
        }
        return instance;
    }

    /**
     * Generates a certificate for a completed enrollment.
     *
     * @return the issued certificate ID, or -1 on failure
     */
    public int generateOnCompletion(int enrollmentId) {
        if (certificateService.existsForEnrollment(enrollmentId)) {
            logger.warn("Certificate already exists for enrollment {}", enrollmentId);
            return -1;
        }

        Enrollment enrollment = enrollmentService.findById(enrollmentId);
        if (enrollment == null) {
            logger.error("Enrollment {} not found", enrollmentId);
            return -1;
        }

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            logger.warn("Enrollment {} is not COMPLETED (status={})", enrollmentId, enrollment.getStatus());
            return -1;
        }

        Formation formation = formationService.findById(enrollment.getFormationId());
        if (formation == null) {
            logger.error("Formation {} not found for enrollment {}", enrollment.getFormationId(), enrollmentId);
            return -1;
        }

        String certNumber = certificateService.generateCertificateNumber();
        LocalDateTime completionDate = enrollment.getCompletedDate() != null
                ? enrollment.getCompletedDate()
                : LocalDateTime.now();

        String hashInput = enrollmentId + "|" + enrollment.getUserId() + "|"
                + enrollment.getFormationId() + "|" + completionDate;
        String hashValue = sha256(hashInput);

        String qrCode = "https://skilora.com/verify/" + certNumber;

        Certificate certificate = new Certificate();
        certificate.setEnrollmentId(enrollmentId);
        certificate.setCertificateNumber(certNumber);
        certificate.setIssuedDate(LocalDateTime.now());
        certificate.setQrCode(qrCode);
        certificate.setHashValue(hashValue);
        certificate.setPdfUrl("/certificates/" + certNumber + ".pdf");

        int certId = certificateService.issue(certificate);
        if (certId > 0) {
            logger.info("Certificate generated: id={}, number={}, enrollment={}",
                    certId, certNumber, enrollmentId);
        } else {
            logger.error("Failed to issue certificate for enrollment {}", enrollmentId);
        }
        return certId;
    }

    /**
     * Completes an enrollment, generates a certificate, and awards a first-certificate
     * achievement if applicable.
     *
     * @return the issued certificate ID, or -1 on failure
     */
    public int completeAndCertify(int enrollmentId) {
        boolean completed = enrollmentService.completeEnrollment(enrollmentId);
        if (!completed) {
            logger.error("Failed to complete enrollment {}", enrollmentId);
            return -1;
        }

        int certId = generateOnCompletion(enrollmentId);
        if (certId <= 0) {
            return certId;
        }

        Enrollment enrollment = enrollmentService.findById(enrollmentId);
        if (enrollment != null) {
            awardFirstCertificateAchievement(enrollment.getUserId());
        }
        return certId;
    }

    // ── Private helpers ──

    private void awardFirstCertificateAchievement(int userId) {
        try {
            if (achievementService.hasAchievement(userId, "FIRST_CERTIFICATE")) {
                return;
            }
            int certCount = certificateService.findByUser(userId).size();
            if (certCount != 1) {
                return;
            }

            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setBadgeType("FIRST_CERTIFICATE");
            achievement.setTitle("First Certificate");
            achievement.setDescription("Earned your first course completion certificate");
            achievement.setIconUrl("/badges/first-certificate.svg");
            achievement.setEarnedDate(LocalDateTime.now());
            achievement.setRarity(BadgeRarity.UNCOMMON);
            achievement.setPoints(50);

            achievementService.award(achievement);
            logger.info("Awarded FIRST_CERTIFICATE achievement to user {}", userId);
        } catch (Exception e) {
            logger.warn("Could not award first-certificate achievement for user {}: {}", userId, e.getMessage());
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
