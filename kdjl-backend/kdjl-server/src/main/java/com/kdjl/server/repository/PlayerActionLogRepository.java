package com.kdjl.server.repository;

import com.kdjl.common.entity.PlayerActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerActionLogRepository extends JpaRepository<PlayerActionLog, Long> {
    @Query("SELECT l FROM PlayerActionLog l WHERE (:playerId IS NULL OR l.playerId = :playerId) AND (:action = '' OR l.action = :action) ORDER BY l.createdAt DESC")
    Page<PlayerActionLog> searchLogs(@Param("playerId") Integer playerId, @Param("action") String action, Pageable pageable);
}
