package com.skilora.model.repository.usermanagement;

import com.skilora.model.entity.usermanagement.Experience;
import java.util.List;
import java.util.Optional;

public interface ExperienceRepository {
    Optional<Experience> findById(int id);
    List<Experience> findByProfileId(int profileId);
    void save(Experience experience);
    void update(Experience experience);
    void delete(int id);
}
