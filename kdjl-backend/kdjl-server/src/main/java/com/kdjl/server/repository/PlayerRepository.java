package com.kdjl.server.repository;

import com.kdjl.common.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Player p WHERE p.username = :username OR p.nickname = :nickname")
    boolean existsByUsernameOrNickname(String username, String nickname);
    Optional<Player> findByNickname(String nickname);

    @Query("SELECT p.id, p.nickname, p.money FROM Player p WHERE p.money > 0 ORDER BY p.money DESC")
    List<Object[]> findTopByMoney();

    @Query("SELECT p.id, p.nickname, p.prestige FROM Player p WHERE p.prestige > 0 ORDER BY p.prestige DESC")
    List<Object[]> findTopByPrestige();

    @Query(value = "SELECT p.id, p.nickname, MAX(b.level) FROM player p JOIN userbb b ON b.uid=p.id GROUP BY p.id ORDER BY MAX(b.level) DESC", nativeQuery = true)
    List<Object[]> findTopByPetLevel();

    @Query(value = "SELECT p.id, p.name, p.nickname, p.vip, p.money, p.yb, p.score, p.prestige, p.jprestige, "
        + "p.active_score, p.viplast, p.inmap, p.openmap, p.fighttop, p.maxbag, p.sex, p.mbid, p.fightbb, "
        + "p.paimoney, p.headimg, p.dblexpflag, p.dblstime, p.maxdblexptime, p.sysautosum, p.maxautofitsum, "
        + "p.friendlist, p.maxmc, pe.onlinetime, pe.new_guide_step, pe.sj, pe.paisj, pe.paiyb, "
        + "pe.merge, pe.hecheng_nums, pe.team_auto_times, pe.tiaozhan, "
        + "(SELECT COUNT(*) FROM userbb b WHERE b.uid = p.id) AS petCount "
        + "FROM player p LEFT JOIN player_ext pe ON pe.uid = p.id WHERE p.id = ?1", nativeQuery = true)
    List<Object[]> findPlayerInfoById(Integer playerId);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.lastVisitTime > :since")
    long countOnlineSince(int since);

    @Modifying
    @Query("UPDATE Player p SET p.lastVisitTime = :now WHERE p.id = :id")
    void updateLastVisitTime(int id, int now);

    List<Player> findByInMapAndLastVisitTimeGreaterThan(Integer inMap, int lastVisitTime);
}
