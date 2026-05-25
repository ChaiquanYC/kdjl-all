package com.kdjl.server.repository;

import com.kdjl.common.entity.WarPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarPlayerRepository extends JpaRepository<WarPlayer, Long> {}
