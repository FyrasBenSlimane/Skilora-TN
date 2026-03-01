package com.skilora.service.usermanagement;

import com.skilora.model.entity.usermanagement.Skill;
import com.skilora.model.repository.usermanagement.SkillRepository;
import com.skilora.model.repository.usermanagement.impl.SkillRepositoryImpl;

import java.util.List;
import java.util.Optional;

public class SkillService {
    private static volatile SkillService instance;
    private final SkillRepository skillRepository;

    private SkillService() {
        this.skillRepository = new SkillRepositoryImpl();
    }

    public static SkillService getInstance() {
        if (instance == null) {
            synchronized (SkillService.class) {
                if (instance == null) {
                    instance = new SkillService();
                }
            }
        }
        return instance;
    }

    public Optional<Skill> getSkillById(int id) {
        return skillRepository.findById(id);
    }

    public List<Skill> getSkillsByProfile(int profileId) {
        return skillRepository.findByProfileId(profileId);
    }

    public void saveSkill(Skill skill) {
        if (skill.getId() != 0) {
            skillRepository.update(skill);
        } else {
            skillRepository.save(skill);
        }
    }

    public void deleteSkill(int id) {
        skillRepository.delete(id);
    }
}
