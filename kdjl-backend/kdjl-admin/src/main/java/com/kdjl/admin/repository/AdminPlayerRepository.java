package com.kdjl.admin.repository;

import com.kdjl.common.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminPlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByUsername(String username);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.lastVisitTime > :since")
    long countOnlineSince(int since);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.lastVisitTime > :start AND p.lastVisitTime <= :end")
    long countOnlineBetween(int start, int end);
}
