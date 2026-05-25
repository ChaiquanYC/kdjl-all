package com.kdjl.server.repository;

import com.kdjl.common.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByPetId(Long petId);
    List<Skill> findByPetIdAndLevelGreaterThan(Long petId, Integer level);
    void deleteByPetId(Long petId);
}
