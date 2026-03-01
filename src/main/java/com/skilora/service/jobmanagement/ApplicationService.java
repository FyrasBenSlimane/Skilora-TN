package com.skilora.service.jobmanagement;

import com.skilora.model.entity.jobmanagement.Application;
import com.skilora.model.repository.jobmanagement.ApplicationRepository;
import com.skilora.model.repository.jobmanagement.impl.ApplicationRepositoryImpl;

import java.util.List;
import java.util.Optional;

public class ApplicationService {
    private static volatile ApplicationService instance;
    private final ApplicationRepository applicationRepository;

    private ApplicationService() {
        this.applicationRepository = new ApplicationRepositoryImpl();
    }

    public static ApplicationService getInstance() {
        if (instance == null) {
            synchronized (ApplicationService.class) {
                if (instance == null) {
                    instance = new ApplicationService();
                }
            }
        }
        return instance;
    }

    public Optional<Application> getApplicationById(int id) {
        return applicationRepository.findById(id);
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public List<Application> getApplicationsByJobOffer(int jobOfferId) {
        return applicationRepository.findByJobOfferId(jobOfferId);
    }

    public List<Application> getApplicationsByCandidate(int profileId) {
        return applicationRepository.findByCandidateProfileId(profileId);
    }

    public void saveApplication(Application application) {
        if (application.getId() != 0) {
            applicationRepository.update(application);
        } else {
            applicationRepository.save(application);
        }
    }

    public void deleteApplication(int id) {
        applicationRepository.delete(id);
    }
}
