package com.kdjl.admin.repository;

import com.kdjl.common.entity.TaskAccept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTaskAcceptRepository extends JpaRepository<TaskAccept, Long> {
    List<TaskAccept> findByPlayerId(Long playerId);
    List<TaskAccept> findByPlayerIdAndTaskId(Long playerId, Long taskId);
    void deleteByPlayerIdAndTaskId(Long playerId, Long taskId);
}
