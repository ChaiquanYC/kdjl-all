package com.kdjl.server.repository;

import com.kdjl.common.entity.PlayerExt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerExtRepository extends JpaRepository<PlayerExt, Integer> {
}
