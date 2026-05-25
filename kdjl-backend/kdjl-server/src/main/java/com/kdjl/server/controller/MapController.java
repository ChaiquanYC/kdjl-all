package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.kdjl.common.entity.GameMap;
import com.kdjl.server.repository.GameMapRepository;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MonsterRepository monsterRepo;
    private final PlayerRepository playerRepo;
    private final GameMapRepository gameMapRepo;
    private final UserBagRepository userBagRepo;

    public MapController(MonsterRepository monsterRepo, PlayerRepository playerRepo,
                         GameMapRepository gameMapRepo, UserBagRepository userBagRepo) {
        this.monsterRepo = monsterRepo;
        this.playerRepo = playerRepo;
        this.gameMapRepo = gameMapRepo;
        this.userBagRepo = userBagRepo;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listMaps(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        Set<String> openMap = new HashSet<>();
        if (player != null && player.getOpenMap() != null) {
            openMap.addAll(Arrays.asList(player.getOpenMap().split(",")));
        }
        openMap.add("1");

        List<Map<String, Object>> maps = new ArrayList<>();
        List<GameMap> dbMaps = gameMapRepo.findAll();
        for (GameMap m : dbMaps) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName());
            map.put("img", m.getImg() != null ? m.getImg() : "gif");
            map.put("gpclist", m.getGpclist() != null ? m.getGpclist() : "");
            map.put("desc", m.getDescs() != null ? m.getDescs() : "");
            map.put("level", m.getLevel() != null ? m.getLevel().replace(",", "-") : "");
            map.put("unlocked", openMap.contains(String.valueOf(m.getId())));
            map.put("needs", m.getNeeds() != null ? m.getNeeds() : "");
            maps.add(map);
        }
        return ApiResponse.success(maps);
    }

    /** Unlock a map — PHP mapGate.php type=2 logic */
    @Transactional
    @PostMapping("/unlock/{mapId}")
    public ApiResponse<Map<String, Object>> unlockMap(@PathVariable Long mapId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        GameMap gameMap = gameMapRepo.findById(mapId.intValue()).orElse(null);
        if (gameMap == null) return ApiResponse.error("地图不存在");

        String needsStr = gameMap.getNeeds();
        if (needsStr == null || needsStr.isEmpty()) {
            // No requirements — just add to openMap
            String current = player.getOpenMap() != null ? player.getOpenMap() : "";
            if (!Arrays.asList(current.split(",")).contains(String.valueOf(mapId))) {
                player.setOpenMap(current + (current.isEmpty() ? "" : ",") + mapId);
                playerRepo.save(player);
            }
            return ApiResponse.success(Map.of("unlocked", true));
        }

        // Parse needs: "needitem:propId|duration" or "needww:prestige|duration"
        String[] parts = needsStr.split(":");
        if (parts.length < 2) return ApiResponse.error("地图配置错误");

        String needType = parts[0];
        String[] needDetail = parts[1].split("\\|");
        int needValue;
        try { needValue = Integer.parseInt(needDetail[0].trim()); } catch (NumberFormatException e) { return ApiResponse.error("地图需求配置错误"); }

        if ("needww".equals(needType)) {
            // PHP: prestige deduction
            int currentPrestige = player.getPrestige() != null ? player.getPrestige() : 0;
            if (currentPrestige < needValue) return ApiResponse.error("威望不足！需要 " + needValue + " 威望");
            player.setPrestige(currentPrestige - needValue);
        } else if ("needitem".equals(needType) || "needtime".equals(needType)) {
            // PHP: consume item from userbag
            var items = userBagRepo.findByPlayerIdAndPropIdIn(uid.intValue(), List.of(needValue));
            UserBag target = null;
            for (UserBag item : items) {
                if (item.getSums() != null && item.getSums() > 0) {
                    target = item;
                    break;
                }
            }
            if (target == null) return ApiResponse.error("缺少解锁道具！需要道具ID: " + needValue);
            target.setSums(target.getSums() - 1);
            userBagRepo.save(target);
        } else {
            return ApiResponse.error("未知的解锁类型: " + needType);
        }

        // Add to openMap
        String openMapStr = player.getOpenMap() != null ? player.getOpenMap() : "";
        if (!Arrays.asList(openMapStr.split(",")).contains(String.valueOf(mapId))) {
            player.setOpenMap(openMapStr + (openMapStr.isEmpty() ? "" : ",") + mapId);
        }
        playerRepo.save(player);
        return ApiResponse.success(Map.of("unlocked", true));
    }

    @GetMapping("/{mapId}/monsters")
    public ApiResponse<List<Map<String, Object>>> getMapMonsters(@PathVariable Long mapId) {
        GameMap gameMap = gameMapRepo.findById(mapId.intValue()).orElse(null);
        int minLevel = 1, maxLevel = 15;
        if (gameMap != null && gameMap.getLevel() != null && gameMap.getLevel().contains(",")) {
            String[] range = gameMap.getLevel().split(",");
            try {
                minLevel = Integer.parseInt(range[0].trim());
                maxLevel = Integer.parseInt(range[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        // Match PHP: WHERE level BETWEEN min AND max AND boss != 4
        List<Monster> monsters = monsterRepo.findByLevelBetweenAndBossNot(minLevel, maxLevel, 4);
        return ApiResponse.success(monsters.stream().map(m -> {
            var mm = new java.util.LinkedHashMap<String, Object>();
            mm.put("id", m.getId()); mm.put("name", m.getName());
            mm.put("level", m.getLevel()); mm.put("hp", m.getHp());
            mm.put("boss", m.getBoss()); mm.put("img", m.getImgstand());
            return mm;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/{mapId}/players")
    public ApiResponse<List<Map<String, Object>>> getMapPlayers(@PathVariable Long mapId) {
        int fiveMinAgo = (int)(System.currentTimeMillis() / 1000) - 300;
        List<Player> players = playerRepo.findByInMapAndLastVisitTimeGreaterThan(mapId.intValue(), fiveMinAgo);
        return ApiResponse.success(players.stream().map(p -> {
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("id", p.getId()); m.put("nickname", p.getNickname());
            m.put("level", p.getScore() != null ? p.getScore() : 0);
            return m;
        }).collect(Collectors.toList()));
    }

}
