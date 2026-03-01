package com.skilora.service.usermanagement;

import com.skilora.model.entity.usermanagement.Experience;
import com.skilora.model.repository.usermanagement.ExperienceRepository;
import com.skilora.model.repository.usermanagement.impl.ExperienceRepositoryImpl;

import java.util.List;
import java.util.Optional;

public class ExperienceService {
    private static volatile ExperienceService instance;
    private final ExperienceRepository experienceRepository;

    private ExperienceService() {
        this.experienceRepository = new ExperienceRepositoryImpl();
    }

    public static ExperienceService getInstance() {
        if (instance == null) {
            synchronized (ExperienceService.class) {
                if (instance == null) {
                    instance = new ExperienceService();
                }
            }
        }
        return instance;
    }

    public Optional<Experience> getExperienceById(int id) {
        return experienceRepository.findById(id);
    }

    public List<Experience> getExperiencesByProfile(int profileId) {
        return experienceRepository.findByProfileId(profileId);
    }

    public void saveExperience(Experience experience) {
        if (experience.getId() != 0) {
            experienceRepository.update(experience);
        } else {
            experienceRepository.save(experience);
        }
    }

    public void deleteExperience(int id) {
        experienceRepository.delete(id);
    }
}
