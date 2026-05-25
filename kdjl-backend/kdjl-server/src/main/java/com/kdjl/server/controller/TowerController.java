package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TongTian Tower (通天塔) — 55 floors with leaderboard.
 * PHP: Tt_Mod.php + tgt field on player_ext
 */
@RestController
@RequestMapping("/api/tower")
public class TowerController {

    private static final int MAX_FLOORS = 55;
    private final MonsterRepository monsterRepo;
    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;

    public TowerController(MonsterRepository monsterRepo, PlayerRepository playerRepo,
                           PlayerExtRepository playerExtRepo) {
        this.monsterRepo = monsterRepo;
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
    }

    /** Get tower state for current player */
    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> getState(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        PlayerExt ext = playerExtRepo.findById(uid.intValue()).orElse(null);
        int currentFloor = ext != null && ext.getTgt() != null ? ext.getTgt() : 0;

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("currentFloor", currentFloor);
        state.put("maxFloor", MAX_FLOORS);
        state.put("canChallenge", currentFloor < MAX_FLOORS);
        return ApiResponse.success(state);
    }

    /** Start tower challenge on next floor */
    @Transactional
    @PostMapping("/challenge")
    public ApiResponse<Map<String, Object>> startChallenge(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        PlayerExt ext = playerExtRepo.findById(uid.intValue()).orElse(null);
        if (ext == null) return ApiResponse.error("玩家数据不存在");

        int difficulty = body.containsKey("difficulty") ? ((Number) body.get("difficulty")).intValue() : 1;
        int currentFloor = ext.getTgt() != null ? ext.getTgt() : 0;
        int nextFloor = currentFloor + 1;
        if (nextFloor > MAX_FLOORS) return ApiResponse.error("已通关所有层！");

        // Tower monster: scale based on floor
        int monsterLevel = 5 + nextFloor * 2;
        List<Monster> candidates = monsterRepo.findByLevelBetweenAndBossNot(
            Math.max(1, monsterLevel - 5), monsterLevel + 5, 4);
        if (candidates.isEmpty()) candidates = monsterRepo.findAll();

        Monster monster = candidates.get(new Random().nextInt(candidates.size()));

        // Apply difficulty scaling to monster HP
        long hpMult = difficulty == 3 ? 2 : difficulty == 2 ? 1 : 1;
        long scaledHp = (monster.getHp() != null ? monster.getHp() : 100) * hpMult;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("floor", nextFloor);
        result.put("monsterId", monster.getId());
        result.put("monsterName", monster.getName());
        result.put("monsterLevel", monster.getLevel());
        result.put("monsterHp", scaledHp);
        result.put("monsterImg", monster.getImgstand());
        result.put("difficulty", difficulty);
        return ApiResponse.success(result);
    }

    /** Record tower floor progress after winning */
    @Transactional
    @PostMapping("/progress")
    public ApiResponse<Map<String, Object>> recordProgress(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int floor = body.containsKey("floor") ? ((Number) body.get("floor")).intValue() : 0;

        PlayerExt ext = playerExtRepo.findById(uid.intValue()).orElse(null);
        if (ext == null) return ApiResponse.error("玩家数据不存在");

        int currentBest = ext.getTgt() != null ? ext.getTgt() : 0;
        if (floor > currentBest) {
            ext.setTgt(floor);
            playerExtRepo.save(ext);
        }

        // Tower rewards: gold per floor
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        int goldReward = floor * 100;
        if (player != null) {
            int current = player.getMoney() != null ? player.getMoney() : 0;
            player.setMoney(current + goldReward);
            playerRepo.save(player);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("floor", floor);
        result.put("bestFloor", ext.getTgt());
        result.put("cleared", floor >= MAX_FLOORS);
        result.put("goldReward", goldReward);
        return ApiResponse.success(result);
    }

    /** Tower leaderboard — top 20 players by tgt (floor progress) */
    @GetMapping("/leaderboard")
    public ApiResponse<List<Map<String, Object>>> leaderboard() {
        List<PlayerExt> top = playerExtRepo.findAll().stream()
            .filter(e -> e.getTgt() != null && e.getTgt() > 0)
            .sorted((a, b) -> Integer.compare(
                b.getTgt() != null ? b.getTgt() : 0,
                a.getTgt() != null ? a.getTgt() : 0))
            .limit(20)
            .collect(Collectors.toList());

        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (PlayerExt e : top) {
            Player p = playerRepo.findById(e.getPlayerId()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("playerId", e.getPlayerId());
            m.put("nickname", p != null ? p.getNickname() : "未知");
            m.put("floor", e.getTgt());
            list.add(m);
        }
        return ApiResponse.success(list);
    }
}
