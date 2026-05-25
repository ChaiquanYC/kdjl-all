package com.kdjl.server.repository;

import com.kdjl.common.entity.WarFighterTalent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WarFighterTalentRepository extends JpaRepository<WarFighterTalent, Long> {
    List<WarFighterTalent> findByFighterId(Long fighterId);
}
