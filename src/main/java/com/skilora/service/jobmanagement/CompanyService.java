package com.skilora.service.jobmanagement;

import com.skilora.model.entity.jobmanagement.Company;
import com.skilora.model.repository.jobmanagement.CompanyRepository;
import com.skilora.model.repository.jobmanagement.impl.CompanyRepositoryImpl;

import java.util.List;
import java.util.Optional;

public class CompanyService {
    private static volatile CompanyService instance;
    private final CompanyRepository companyRepository;

    private CompanyService() {
        this.companyRepository = new CompanyRepositoryImpl();
    }

    public static CompanyService getInstance() {
        if (instance == null) {
            synchronized (CompanyService.class) {
                if (instance == null) {
                    instance = new CompanyService();
                }
            }
        }
        return instance;
    }

    public Optional<Company> getCompanyById(int id) {
        return companyRepository.findById(id);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public List<Company> getCompaniesByOwner(int ownerId) {
        return companyRepository.findByOwnerId(ownerId);
    }

    public void saveCompany(Company company) {
        if (company.getId() != 0) {
            companyRepository.update(company);
        } else {
            companyRepository.save(company);
        }
    }

    public void deleteCompany(int id) {
        companyRepository.delete(id);
    }
}
