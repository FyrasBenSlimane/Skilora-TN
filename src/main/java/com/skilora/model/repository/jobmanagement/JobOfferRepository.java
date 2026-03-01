package com.skilora.model.repository.jobmanagement;

import com.skilora.model.entity.jobmanagement.JobOffer;
import java.util.List;
import java.util.Optional;

public interface JobOfferRepository {
    Optional<JobOffer> findById(int id);
    List<JobOffer> findAll();
    List<JobOffer> findByCompanyId(int companyId);
    void save(JobOffer jobOffer);
    void update(JobOffer jobOffer);
    void delete(int id);
}
