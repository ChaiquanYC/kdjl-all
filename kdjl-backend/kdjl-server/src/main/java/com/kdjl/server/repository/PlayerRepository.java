package com.kdjl.server.repository;

import com.kdjl.common.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByUsernameAndSecret(String username, String secret);
    Optional<Player> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    Optional<Player> findByNickname(String nickname);

    @Query("SELECT p.id, p.nickname, p.money FROM Player p WHERE p.money > 0 ORDER BY p.money DESC")
    List<Object[]> findTopByMoney();

    @Query("SELECT p.id, p.nickname, p.prestige FROM Player p WHERE p.prestige > 0 ORDER BY p.prestige DESC")
    List<Object[]> findTopByPrestige();

    @Query(value = "SELECT p.id, p.nickname, MAX(b.level) FROM player p JOIN userbb b ON b.uid=p.id GROUP BY p.id ORDER BY MAX(b.level) DESC", nativeQuery = true)
    List<Object[]> findTopByPetLevel();

    @Query("SELECT COUNT(p) FROM Player p WHERE p.lastVisitTime > :since")
    long countOnlineSince(int since);

    List<Player> findByInMapAndLastVisitTimeGreaterThan(Integer inMap, int lastVisitTime);
}
