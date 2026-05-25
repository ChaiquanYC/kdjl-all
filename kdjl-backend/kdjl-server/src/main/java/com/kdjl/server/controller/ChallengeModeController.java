package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Challenge mode (挑战模式, map 125 琥珀屋). PHP: multi_monsters=1
 * Daily 3 free attempts, star difficulty (1-5), c_gpc monsters.
 */
@RestController
@RequestMapping("/api/challenge")
public class ChallengeModeController {

    private final ChallengeRepository challengeRepo;
    private final MonsterRepository monsterRepo;
    private final PlayerRepository playerRepo;

    public ChallengeModeController(ChallengeRepository challengeRepo,
                                   MonsterRepository monsterRepo, PlayerRepository playerRepo) {
        this.challengeRepo = challengeRepo;
        this.monsterRepo = monsterRepo;
        this.playerRepo = playerRepo;
    }

    /** Get challenge state for current player */
    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> getState(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        var c = challengeRepo.findByPlayerId(uid).orElse(null);
        Map<String, Object> state = new LinkedHashMap<>();
        int maxAttempts = 3;
        if (c != null && c.getSnums() != null) maxAttempts = c.getSnums();
        int used = c != null && c.getNums() != null ? c.getNums() : 0;
        // PHP: daily reset based on lastvtime date comparison
        if (c != null && c.getLastvtime() != null) {
            long now = System.currentTimeMillis() / 1000;
            java.util.Date lastDate = new java.util.Date(c.getLastvtime() * 1000);
            java.util.Date today = new java.util.Date();
            if (lastDate.getDate() != today.getDate() || lastDate.getMonth() != today.getMonth()) {
                used = 0; // new day, reset
            }
        }
        state.put("maxAttempts", maxAttempts);
        state.put("usedAttempts", used);
        state.put("remainingAttempts", Math.max(0, maxAttempts - used));
        state.put("difficulty", c != null && c.getVary() != null ? c.getVary() : 1);
        return ApiResponse.success(state);
    }

    /** Start a challenge fight */
    @Transactional
    @PostMapping("/enter")
    public ApiResponse<Map<String, Object>> enterChallenge(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int difficulty = body.containsKey("difficulty") ? ((Number) body.get("difficulty")).intValue() : 1;
        difficulty = Math.max(1, Math.min(5, difficulty));

        var c = challengeRepo.findByPlayerId(uid).orElse(null);
        int maxAttempts = 3;
        if (c != null && c.getSnums() != null) maxAttempts = c.getSnums();
        int used = c != null && c.getNums() != null ? c.getNums() : 0;

        // Daily reset check
        if (c != null && c.getLastvtime() != null) {
            java.util.Date lastDate = new java.util.Date(c.getLastvtime() * 1000);
            java.util.Date today = new java.util.Date();
            if (lastDate.getDate() != today.getDate() || lastDate.getMonth() != today.getMonth()) {
                used = 0;
            }
        }

        if (used >= maxAttempts) return ApiResponse.error("今日挑战次数已用完！");

        // PHP: c_gpc table for challenge monsters, fallback to regular monsters by level
        int monsterLevel = 10 + difficulty * 20;
        List<Monster> candidates = monsterRepo.findByLevelBetweenAndBossNot(
            Math.max(1, monsterLevel - 10), monsterLevel + 10, 4);
        if (candidates.isEmpty()) candidates = monsterRepo.findAll().stream().limit(20).toList();
        Monster monster = candidates.get(new Random().nextInt(candidates.size()));

        // Record attempt
        if (c == null) {
            c = new Challenge();
        }
        c.setPlayerId(uid);
        c.setLastvtime(System.currentTimeMillis() / 1000);
        c.setNums(used + 1);
        c.setMonsterId(monster.getId());
        c.setVary(difficulty);
        if (c.getSnums() == null) c.setSnums(maxAttempts);
        challengeRepo.save(c);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monsterId", monster.getId());
        result.put("monsterName", monster.getName());
        result.put("monsterLevel", monster.getLevel());
        result.put("monsterHp", monster.getHp() != null ? monster.getHp() * difficulty : 100);
        result.put("difficulty", difficulty);
        result.put("attempt", used + 1);
        return ApiResponse.success(result);
    }
}
