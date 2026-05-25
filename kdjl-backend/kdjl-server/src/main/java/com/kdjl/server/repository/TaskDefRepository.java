package com.kdjl.server.repository;

import com.kdjl.common.entity.TaskDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDefRepository extends JpaRepository<TaskDef, Long> {}
