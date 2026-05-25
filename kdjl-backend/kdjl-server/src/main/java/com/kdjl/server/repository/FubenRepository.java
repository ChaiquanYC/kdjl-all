package com.kdjl.server.repository;

import com.kdjl.common.entity.Fuben;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FubenRepository extends JpaRepository<Fuben, Long> {
    Optional<Fuben> findByPlayerIdAndInmap(Long playerId, Integer inmap);
    List<Fuben> findByPlayerId(Long playerId);
}
