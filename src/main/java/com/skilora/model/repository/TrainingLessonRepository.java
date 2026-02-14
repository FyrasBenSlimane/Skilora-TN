package com.skilora.model.repository;

import com.skilora.model.entity.formation.TrainingLesson;
import java.util.List;
import java.util.Optional;

public interface TrainingLessonRepository {
    TrainingLesson save(TrainingLesson lesson);
    Optional<TrainingLesson> findById(int id);
    List<TrainingLesson> findByTrainingId(int trainingId);
    boolean delete(int id);
}
