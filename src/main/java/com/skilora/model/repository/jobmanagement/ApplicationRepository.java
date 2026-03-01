package com.skilora.model.repository.jobmanagement;

import com.skilora.model.entity.jobmanagement.Application;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Optional<Application> findById(int id);
    List<Application> findAll();
    List<Application> findByJobOfferId(int jobOfferId);
    List<Application> findByCandidateProfileId(int profileId);
    void save(Application application);
    void update(Application application);
    void delete(int id);
}
