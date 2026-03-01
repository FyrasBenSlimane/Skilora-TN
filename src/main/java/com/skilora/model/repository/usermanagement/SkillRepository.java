package com.skilora.model.repository.usermanagement;

import com.skilora.model.entity.usermanagement.Skill;
import java.util.List;
import java.util.Optional;

public interface SkillRepository {
    Optional<Skill> findById(int id);
    List<Skill> findByProfileId(int profileId);
    void save(Skill skill);
    void update(Skill skill);
    void delete(int id);
}
