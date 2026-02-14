package com.skilora.model.repository;

import com.skilora.model.entity.formation.TrainingEnrollment;

import java.util.List;
import java.util.Optional;

public interface TrainingEnrollmentRepository {
    TrainingEnrollment save(TrainingEnrollment enrollment);
    Optional<TrainingEnrollment> findById(int id);
    Optional<TrainingEnrollment> findByUserIdAndTrainingId(int userId, int trainingId);
    List<TrainingEnrollment> findByUserId(int userId);
    List<TrainingEnrollment> findByTrainingId(int trainingId);
    boolean delete(int id);
    boolean existsByUserIdAndTrainingId(int userId, int trainingId);
}
