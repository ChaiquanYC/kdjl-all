package com.kdjl.server.repository;

import com.kdjl.common.entity.Monster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonsterRepository extends JpaRepository<Monster, Long> {
    Optional<Monster> findByName(String name);
    List<Monster> findByLevelBetween(Integer minLevel, Integer maxLevel);
    List<Monster> findByLevelBetweenAndBossNot(Integer minLevel, Integer maxLevel, Integer boss);
    List<Monster> findByBoss(Integer boss);
}
