package com.kdjl.admin.repository;

import com.kdjl.common.entity.PlayerExt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPlayerExtRepository extends JpaRepository<PlayerExt, Integer> {}
