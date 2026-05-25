package com.kdjl.server.repository;

import com.kdjl.common.entity.WarFighter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WarFighterRepository extends JpaRepository<WarFighter, WarFighter.WarFighterId> {
    List<WarFighter> findByUserId(Long userId);
}
