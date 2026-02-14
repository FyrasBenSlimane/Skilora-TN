package com.skilora.service.formation;

import com.skilora.model.entity.formation.TrainingEnrollment;
import com.skilora.model.repository.TrainingEnrollmentRepository;
import com.skilora.model.repository.impl.TrainingEnrollmentRepositoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * TrainingEnrollmentService
 * 
 * Handles business logic for training enrollments.
 */
public class TrainingEnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingEnrollmentService.class);
    
    private final TrainingEnrollmentRepository repository;
    private static volatile TrainingEnrollmentService instance;

    private TrainingEnrollmentService() {
        this.repository = new TrainingEnrollmentRepositoryImpl();
    }

    public static TrainingEnrollmentService getInstance() {
        if (instance == null) {
            synchronized (TrainingEnrollmentService.class) {
                if (instance == null) {
                    instance = new TrainingEnrollmentService();
                }
            }
        }
        return instance;
    }

    /**
     * Enroll a user in a training
     */
    public TrainingEnrollment enrollUser(int userId, int trainingId) {
        // Check if already enrolled
        if (repository.existsByUserIdAndTrainingId(userId, trainingId)) {
            throw new IllegalArgumentException("User is already enrolled in this training");
        }
        
        TrainingEnrollment enrollment = new TrainingEnrollment(userId, trainingId);
        return repository.save(enrollment);
    }

    /**
     * Check if user is enrolled in a training
     */
    public boolean isEnrolled(int userId, int trainingId) {
        return repository.existsByUserIdAndTrainingId(userId, trainingId);
    }

    /**
     * Get enrollment by user and training
     */
    public Optional<TrainingEnrollment> getEnrollment(int userId, int trainingId) {
        return repository.findByUserIdAndTrainingId(userId, trainingId);
    }

    /**
     * Get all enrollments for a user
     */
    public List<TrainingEnrollment> getUserEnrollments(int userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed(int userId, int trainingId) {
        Optional<TrainingEnrollment> enrollmentOpt = repository.findByUserIdAndTrainingId(userId, trainingId);
        if (enrollmentOpt.isPresent()) {
            TrainingEnrollment enrollment = enrollmentOpt.get();
            enrollment.setLastAccessedAt(java.time.LocalDateTime.now());
            repository.save(enrollment);
        }
    }

    /**
     * Mark training as completed and automatically generate certificate
     */
    public void markCompleted(int userId, int trainingId) {
        logger.info("Marking training as completed: user={}, training={}", userId, trainingId);
        Optional<TrainingEnrollment> enrollmentOpt = repository.findByUserIdAndTrainingId(userId, trainingId);
        if (enrollmentOpt.isPresent()) {
            TrainingEnrollment enrollment = enrollmentOpt.get();
            enrollment.setCompleted(true);
            repository.save(enrollment);
            logger.info("Training marked as completed in database: user={}, training={}", userId, trainingId);
            
            // Automatically generate certificate if it doesn't exist
            try {
                CertificateService certService = CertificateService.getInstance();
                if (!certService.certificateExists(userId, trainingId)) {
                    logger.info("Auto-generating certificate after completion: user={}, training={}", userId, trainingId);
                    certService.createCertificate(userId, trainingId);
                } else {
                    logger.debug("Certificate already exists, skipping generation: user={}, training={}", userId, trainingId);
                }
            } catch (Exception e) {
                logger.error("Error auto-generating certificate after completion: user={}, training={}, error={}", 
                    userId, trainingId, e.getMessage(), e);
                // Don't throw - completion should succeed even if certificate generation fails
            }
        } else {
            logger.warn("Cannot mark training as completed - enrollment not found: user={}, training={}", 
                userId, trainingId);
        }
    }
}
