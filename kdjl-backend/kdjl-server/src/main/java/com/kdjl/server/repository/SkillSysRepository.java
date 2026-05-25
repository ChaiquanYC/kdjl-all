package com.kdjl.server.repository;

import com.kdjl.common.entity.SkillSys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillSysRepository extends JpaRepository<SkillSys, Long> {
    List<SkillSys> findByPid(Long petTemplateId);
}
