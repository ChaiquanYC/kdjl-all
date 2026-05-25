package com.kdjl.admin.repository;

import com.kdjl.common.entity.FightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminFightLogRepository extends JpaRepository<FightLog, Long> {
    List<FightLog> findByPlayerIdOrderByTimeDesc(Long playerId);
}
