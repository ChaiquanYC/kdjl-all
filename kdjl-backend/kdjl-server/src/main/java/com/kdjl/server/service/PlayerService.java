package com.kdjl.server.service;

import com.kdjl.common.entity.Player;
import com.kdjl.common.entity.PlayerExt;
import com.kdjl.common.entity.UserPet;
import com.kdjl.server.repository.PlayerExtRepository;
import com.kdjl.server.repository.PlayerRepository;
import com.kdjl.server.repository.UserPetRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
        Player p = getPlayer(playerId);
        PlayerExt ext = getPlayerExt(playerId);
        var m = new LinkedHashMap<String, Object>();
        m.put("id", p.getId());
        m.put("username", p.getUsername());
        m.put("nickname", p.getNickname());
        m.put("vip", p.getVip() != null ? p.getVip() : 0);
        m.put("money", p.getMoney() != null ? p.getMoney() : 0);
        m.put("yb", p.getYb() != null ? p.getYb() : 0);
        m.put("score", p.getScore() != null ? p.getScore() : 0);
        m.put("prestige", p.getPrestige() != null ? p.getPrestige() : 0);
        m.put("jPrestige", p.getJPrestige() != null ? p.getJPrestige() : 0);
        m.put("activeScore", p.getActiveScore() != null ? p.getActiveScore() : 0);
        m.put("vipLast", p.getVipLast() != null ? p.getVipLast() : 0);
        m.put("inMap", p.getInMap() != null ? p.getInMap() : 0);
        m.put("openMap", p.getOpenMap());
        m.put("fightTop", p.getFightTop() != null ? p.getFightTop() : 0);
        m.put("maxBag", p.getMaxBag() != null ? p.getMaxBag() : 30);
        m.put("sex", p.getSex());
        m.put("onlineTime", ext != null && ext.getOnlineTime() != null ? ext.getOnlineTime() : 0);
        m.put("newGuideStep", ext != null && ext.getNewGuideStep() != null ? ext.getNewGuideStep() : 0);
        m.put("mbid", p.getMbid());
        m.put("fightbb", p.getFightBb());
        m.put("sj", ext != null && ext.getSj() != null ? ext.getSj() : 0);
        m.put("merge", ext != null && ext.getMerge() != null ? ext.getMerge() : 0);
        m.put("mergeCount", ext != null && ext.getMergeCount() != null ? ext.getMergeCount() : 0);
        m.put("maxMc", p.getMaxMc() != null ? p.getMaxMc() : 10);
        m.put("headImg", p.getHeadImg() != null ? p.getHeadImg() : 0);
        m.put("dblExpFlag", p.getDblExpFlag() != null ? p.getDblExpFlag() : 0);
        m.put("dblsTime", p.getDblsTime());
        m.put("maxDblExpTime", p.getMaxDblExpTime());
        m.put("sysAutoSum", p.getSysAutoSum() != null ? p.getSysAutoSum() : 0);
        m.put("maxAutoFitSum", p.getMaxAutoFitSum() != null ? p.getMaxAutoFitSum() : 0);
        m.put("friendList", p.getFriendList());
        m.put("teamAutoTimes", ext != null && ext.getTeamAutoTimes() != null ? ext.getTeamAutoTimes() : 0);
        m.put("tiaozhan", ext != null && ext.getTiaozhan() != null ? ext.getTiaozhan() : 1);
        // Pet count
        long petCount = userPetRepo.findByPlayerId(playerId.longValue()).size();
        m.put("petCount", (int) petCount);
        return m;
    }

    public long getOnlineCount() {
        int fiveMinutesAgo = (int) (System.currentTimeMillis() / 1000) - 300;
        return playerRepo.countOnlineSince(fiveMinutesAgo);
    }

    public void updateOnlineStatus(Integer playerId) {
        cache.setPlayerOnline(playerId);
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
