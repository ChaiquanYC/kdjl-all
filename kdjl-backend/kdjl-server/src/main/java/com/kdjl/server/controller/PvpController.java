package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import com.kdjl.server.service.LevelUpService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PvP Battlefield (神圣战场).
 * PHP: ChallengeGate.php guild/fortress PvP.
 */
@RestController
@RequestMapping("/api/pvp")
public class PvpController {

    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private final UserPetRepository userPetRepo;
    private final LevelUpService levelUpService;

    public PvpController(PlayerRepository playerRepo, PlayerExtRepository playerExtRepo,
                         UserPetRepository userPetRepo, LevelUpService levelUpService) {
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.userPetRepo = userPetRepo;
        this.levelUpService = levelUpService;
    }

    /** List players available for PvP challenge (online + has main pet) */
    @GetMapping("/opponents")
    public ApiResponse<List<Map<String, Object>>> listOpponents(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int fiveMinAgo = (int)(System.currentTimeMillis() / 1000) - 300;

        List<Map<String, Object>> list = new ArrayList<>();
        List<Player> online = playerRepo.findAll().stream()
            .filter(p -> !p.getId().equals(uid.intValue()))
            .filter(p -> p.getLastVisitTime() != null && p.getLastVisitTime() > fiveMinAgo)
            .filter(p -> p.getMbid() != null)
            .limit(30)
            .toList();

        for (Player p : online) {
            UserPet mainPet = userPetRepo.findById((long) p.getMbid()).orElse(null);
            if (mainPet == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("nickname", p.getNickname());
            m.put("level", p.getScore() != null ? p.getScore() : 0);
            m.put("petName", mainPet.getName());
            m.put("petLevel", mainPet.getLevel());
            m.put("prestige", p.getPrestige() != null ? p.getPrestige() : 0);
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    /** Execute PvP battle: auto-resolution between two main pets */
    @Transactional
    @PostMapping("/challenge/{opponentId}")
    public ApiResponse<Map<String, Object>> challenge(@PathVariable Long opponentId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        if (uid.longValue() == opponentId) return ApiResponse.error("不能挑战自己");

        Player attacker = playerRepo.findById(uid.intValue()).orElse(null);
        Player defender = playerRepo.findById(opponentId.intValue()).orElse(null);
        if (attacker == null || defender == null) return ApiResponse.error("玩家不存在");

        UserPet atkPet = attacker.getMbid() != null ? userPetRepo.findById((long) attacker.getMbid()).orElse(null) : null;
        UserPet defPet = defender.getMbid() != null ? userPetRepo.findById((long) defender.getMbid()).orElse(null) : null;
        if (atkPet == null || defPet == null) return ApiResponse.error("对方没有设置主战宠物");

        // Auto-battle: simplified PvP resolution
        long atkHp = atkPet.getHp() != null ? atkPet.getHp() : 100;
        long defHp = defPet.getHp() != null ? defPet.getHp() : 100;
        long atkDmg = Math.max(5, (atkPet.getAc() != null ? atkPet.getAc() : 10) + (atkPet.getLevel() != null ? atkPet.getLevel() : 1) * 2);
        long defDmg = Math.max(5, (defPet.getAc() != null ? defPet.getAc() : 10) + (defPet.getLevel() != null ? defPet.getLevel() : 1) * 2);

        List<Map<String, Object>> rounds = new ArrayList<>();
        boolean won = false;
        for (int r = 1; r <= 20; r++) {
            // Attacker hits
            long atkHit = Math.max(1, atkDmg + ThreadLocalRandom.current().nextInt(-5, 6));
            defHp = Math.max(0, defHp - atkHit);
            Map<String, Object> atkRound = new LinkedHashMap<>();
            atkRound.put("round", r); atkRound.put("attacker", atkPet.getName());
            atkRound.put("damage", atkHit); atkRound.put("defenderHp", defHp);
            rounds.add(atkRound);
            if (defHp <= 0) { won = true; break; }

            // Defender hits
            long defHit = Math.max(1, defDmg + ThreadLocalRandom.current().nextInt(-5, 6));
            atkHp = Math.max(0, atkHp - defHit);
            Map<String, Object> defRound = new LinkedHashMap<>();
            defRound.put("round", r); defRound.put("attacker", defPet.getName());
            defRound.put("damage", defHit); defRound.put("attackerHp", atkHp);
            rounds.add(defRound);
            if (atkHp <= 0) break;
        }

        // Apply results
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("won", won);
        result.put("rounds", rounds);
        result.put("atkPetName", atkPet.getName());
        result.put("defPetName", defPet.getName());

        if (won) {
            // Winner gets prestige + EXP
            int prestige = (attacker.getPrestige() != null ? attacker.getPrestige() : 0) + 10;
            attacker.setPrestige(prestige);
            long expGain = defPet.getLevel() != null ? defPet.getLevel() * 50L : 100;
            levelUpService.addExp(atkPet, expGain);
            result.put("expGained", expGain);
            result.put("prestigeGained", 10);
        } else {
            // Loser still gets small EXP
            long expGain = Math.max(10, defPet.getLevel() != null ? defPet.getLevel() * 10 : 10);
            levelUpService.addExp(atkPet, expGain);
            result.put("expGained", expGain);
        }
        long maxHp = (atkPet.getSrchp() != null ? atkPet.getSrchp() : 0) + (atkPet.getAddhp() != null ? atkPet.getAddhp() : 0);
        atkPet.setHp(maxHp > 0 ? maxHp : atkHp);
        playerRepo.save(attacker);
        userPetRepo.save(atkPet);
        result.put("newPrestige", attacker.getPrestige());

        return ApiResponse.success(result);
    }

    /** PvP leaderboard by prestige */
    @GetMapping("/leaderboard")
    public ApiResponse<List<Map<String, Object>>> leaderboard() {
        var top = playerRepo.findTopByPrestige();
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < Math.min(20, top.size()); i++) {
            Object[] row = top.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", i + 1);
            m.put("playerId", row[0]);
            m.put("nickname", row[1]);
            m.put("prestige", row[2]);
            list.add(m);
        }
        return ApiResponse.success(list);
    }
}
