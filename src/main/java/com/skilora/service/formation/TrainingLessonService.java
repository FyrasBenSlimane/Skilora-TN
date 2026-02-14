package com.skilora.service.formation;

import com.skilora.model.entity.formation.TrainingLesson;
import com.skilora.model.repository.TrainingLessonRepository;
import com.skilora.model.repository.impl.TrainingLessonRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * TrainingLessonService
 * 
 * Handles business logic for training lessons.
 */
public class TrainingLessonService {
    private static final Logger logger = LoggerFactory.getLogger(TrainingLessonService.class);
    private final TrainingLessonRepository repository;
    private static volatile TrainingLessonService instance;

    private TrainingLessonService() {
        this.repository = new TrainingLessonRepositoryImpl();
    }

    public static TrainingLessonService getInstance() {
        if (instance == null) {
            synchronized (TrainingLessonService.class) {
                if (instance == null) instance = new TrainingLessonService();
            }
        }
        return instance;
    }

    public List<TrainingLesson> getLessonsByTrainingId(int trainingId) {
        return repository.findByTrainingId(trainingId);
    }
}
