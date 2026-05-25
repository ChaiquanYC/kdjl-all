package com.kdjl.server.repository;

import com.kdjl.common.entity.GameMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameMapRepository extends JpaRepository<GameMap, Integer> {
}
