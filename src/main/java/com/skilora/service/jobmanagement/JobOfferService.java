package com.skilora.service.jobmanagement;

import com.skilora.model.entity.jobmanagement.JobOffer;
import com.skilora.model.repository.jobmanagement.JobOfferRepository;
import com.skilora.model.repository.jobmanagement.impl.JobOfferRepositoryImpl;

import java.util.List;
import java.util.Optional;

public class JobOfferService {
    private static volatile JobOfferService instance;
    private final JobOfferRepository jobOfferRepository;

    private JobOfferService() {
        this.jobOfferRepository = new JobOfferRepositoryImpl();
    }

    public static JobOfferService getInstance() {
        if (instance == null) {
            synchronized (JobOfferService.class) {
                if (instance == null) {
                    instance = new JobOfferService();
                }
            }
        }
        return instance;
    }

    public Optional<JobOffer> getJobOfferById(int id) {
        return jobOfferRepository.findById(id);
    }

    public List<JobOffer> getAllJobOffers() {
        return jobOfferRepository.findAll();
    }

    public List<JobOffer> getJobOffersByCompany(int companyId) {
        return jobOfferRepository.findByCompanyId(companyId);
    }

    public void saveJobOffer(JobOffer jobOffer) {
        if (jobOffer.getId() != 0) {
            jobOfferRepository.update(jobOffer);
        } else {
            jobOfferRepository.save(jobOffer);
        }
    }

    public void deleteJobOffer(int id) {
        jobOfferRepository.delete(id);
    }
}
