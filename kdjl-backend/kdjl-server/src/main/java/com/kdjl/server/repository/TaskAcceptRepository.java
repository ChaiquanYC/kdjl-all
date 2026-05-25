package com.kdjl.server.repository;

import com.kdjl.common.entity.TaskAccept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAcceptRepository extends JpaRepository<TaskAccept, Long> {
    List<TaskAccept> findByPlayerId(Long playerId);
    Optional<TaskAccept> findByPlayerIdAndTaskId(Long playerId, Long taskId);
}
