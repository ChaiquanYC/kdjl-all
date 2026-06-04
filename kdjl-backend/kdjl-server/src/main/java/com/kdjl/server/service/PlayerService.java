package com.kdjl.server.service;

import com.kdjl.common.entity.Player;
import com.kdjl.common.entity.PlayerExt;
import com.kdjl.common.entity.UserPet;
import com.kdjl.server.repository.PlayerExtRepository;
import com.kdjl.server.repository.PlayerRepository;
import com.kdjl.server.repository.UserPetRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private final CacheService cache;
    private final UserPetRepository userPetRepo;

    public PlayerService(PlayerRepository playerRepo,
                         PlayerExtRepository playerExtRepo,
                         CacheService cache,
                         UserPetRepository userPetRepo) {
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.cache = cache;
        this.userPetRepo = userPetRepo;
    }

    @Cacheable(value = "player", key = "#playerId")
    public Player getPlayer(Integer playerId) {
        return playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在: " + playerId));
    }

    public PlayerExt getPlayerExt(Integer playerId) {
        return playerExtRepo.findById(playerId).orElseGet(() -> {
            PlayerExt ext = new PlayerExt();
            ext.setPlayerId(playerId); ext.setSj(0); ext.setMerge(0);
            ext.setRequestMerge(0); ext.setRequest(0);
            return playerExtRepo.save(ext);
        });
    }

    public Map<String, Object> getPlayerInfo(Integer playerId) {
        Object[] row = playerRepo.findPlayerInfoById(playerId).get(0);
        var m = new LinkedHashMap<String, Object>();
        m.put("id", row[0]);
        m.put("username", row[1]);
        m.put("nickname", row[2]);
        m.put("vip", orZero(row[3]));
        m.put("money", orZero(row[4]));
        m.put("yb", orZero(row[5]));
        m.put("score", orZero(row[6]));
        m.put("prestige", orZero(row[7]));
        m.put("jPrestige", orZero(row[8]));
        m.put("activeScore", orZero(row[9]));
        m.put("vipLast", orZero(row[10]));
        m.put("inMap", orZero(row[11]));
        m.put("openMap", row[12]);
        m.put("fightTop", orZero(row[13]));
        m.put("maxBag", row[14] != null ? row[14] : 30);
        m.put("sex", row[15]);
        m.put("mbid", row[16]);
        m.put("fightbb", row[17]);
        m.put("paimoney", orZero(row[18]));
        m.put("headImg", orZero(row[19]));
        m.put("dblExpFlag", orZero(row[20]));
        m.put("dblsTime", row[21]);
        m.put("maxDblExpTime", row[22]);
        m.put("sysAutoSum", orZero(row[23]));
        m.put("maxAutoFitSum", orZero(row[24]));
        m.put("friendList", row[25]);
        m.put("maxMc", row[26] != null ? row[26] : 10);
        m.put("onlineTime", orZero(row[27]));
        m.put("newGuideStep", orZero(row[28]));
        m.put("sj", orZero(row[29]));
        m.put("paisj", orZero(row[30]));
        m.put("paiyb", orZero(row[31]));
        m.put("merge", orZero(row[32]));
        m.put("mergeCount", orZero(row[33]));
        m.put("teamAutoTimes", orZero(row[34]));
        m.put("tiaozhan", row[35] != null ? row[35] : 1);
        m.put("petCount", row[36] != null ? ((Number) row[36]).intValue() : 0);
        return m;
    }

    private static int orZero(Object v) {
        return v != null ? ((Number) v).intValue() : 0;
    }

    public long getOnlineCount() {
        int fiveMinutesAgo = (int) (System.currentTimeMillis() / 1000) - 300;
        return playerRepo.countOnlineSince(fiveMinutesAgo);
    }

    @Transactional
    public void updateOnlineStatus(Integer playerId) {
        cache.setPlayerOnline(playerId);
        playerRepo.updateLastVisitTime(playerId, (int) (System.currentTimeMillis() / 1000));
    }

    @Transactional
    public void updateLastVisitTime(Integer playerId) {
        playerRepo.updateLastVisitTime(playerId, (int) (System.currentTimeMillis() / 1000));
    }

    public void enterMap(Integer playerId, Integer mapId) {
        Player p = getPlayer(playerId);
        if (mapId > 1) {
            String openMap = p.getOpenMap() != null ? p.getOpenMap() : "1";
            if (!Arrays.asList(openMap.split(",")).contains(String.valueOf(mapId))) {
                throw new IllegalArgumentException("该地图未解锁");
            }
        }
        p.setInMap(mapId);
        playerRepo.save(p);
    }

    public void leaveMap(Integer playerId) {
        Player p = getPlayer(playerId);
        p.setInMap(0);
        playerRepo.save(p);
        // Auto-heal all carried pets when returning to town
        List<UserPet> pets = userPetRepo.findByPlayerId(playerId.longValue()).stream()
            .filter(pet -> pet.getMuchang() == null || pet.getMuchang() == 1).collect(Collectors.toList());
        for (UserPet pet : pets) {
            long maxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
            long maxMp = (pet.getSrcmp() != null ? pet.getSrcmp() : 100) + (pet.getAddmp() != null ? pet.getAddmp() : 0);
            pet.setHp(maxHp); pet.setMp(maxMp);
            userPetRepo.save(pet);
        }
    }
}
