package com.skilora.model.repository.jobmanagement;

import com.skilora.model.entity.jobmanagement.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Optional<Company> findById(int id);
    List<Company> findAll();
    List<Company> findByOwnerId(int ownerId);
    void save(Company company);
    void update(Company company);
    void delete(int id);
}
