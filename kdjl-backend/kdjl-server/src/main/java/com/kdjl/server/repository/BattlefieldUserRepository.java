package com.kdjl.server.repository;

import com.kdjl.common.entity.BattlefieldUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BattlefieldUserRepository extends JpaRepository<BattlefieldUser, Long> {
    Optional<BattlefieldUser> findByPlayerId(Long playerId);
}
