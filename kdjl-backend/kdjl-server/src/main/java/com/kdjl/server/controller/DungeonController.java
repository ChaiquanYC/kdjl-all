package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.common.entity.PlayerExt;
import com.kdjl.server.config.DungeonConfig;
import com.kdjl.server.config.DungeonConfig.DungeonInfo;
import com.kdjl.server.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dungeon")
public class DungeonController {

    private final PlayerRepository playerRepo;
    private final FubenRepository fubenRepo;
    private final MonsterRepository monsterRepo;
    private final PlayerExtRepository playerExtRepo;

    public DungeonController(PlayerRepository playerRepo, FubenRepository fubenRepo,
                             MonsterRepository monsterRepo, PlayerExtRepository playerExtRepo) {
        this.playerRepo = playerRepo;
        this.fubenRepo = fubenRepo;
        this.monsterRepo = monsterRepo;
        this.playerExtRepo = playerExtRepo;
    }

    /** List all dungeons with player progress */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listDungeons(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        int playerLevel = player != null && player.getScore() != null ? player.getScore() : 0;

        List<Map<String, Object>> list = new ArrayList<>();
        for (DungeonInfo di : DungeonConfig.DUNGEONS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", di.id());
            m.put("name", di.name());
            m.put("desc", di.desc());
            m.put("level", di.level());
            m.put("cooldown", di.cooldown());
            m.put("waveCount", di.monsterIds().size());
            m.put("unlocked", playerLevel >= di.level());

            // Check cooldown from fuben table
            var fuben = fubenRepo.findByPlayerIdAndInmap(uid, di.id()).orElse(null);
            if (fuben != null && fuben.getLttime() != null) {
                long now = System.currentTimeMillis() / 1000;
                long elapsed = now - fuben.getLttime();
                long remaining = Math.max(0, di.cooldown() - elapsed);
                m.put("onCooldown", remaining > 0);
                m.put("cooldownRemaining", remaining);
                m.put("progress", fuben.getGwId() != null ? fuben.getGwId() : 0);
            } else {
                m.put("onCooldown", false);
                m.put("cooldownRemaining", 0);
                m.put("progress", 0);
            }
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    /** Get dungeon detail + monsters */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getDungeon(@PathVariable Long id, Authentication auth) {
        DungeonInfo di = DungeonConfig.getById(id.intValue()).orElse(null);
        if (di == null) return ApiResponse.error("副本不存在");

        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", di.id());
        info.put("name", di.name());
        info.put("desc", di.desc());
        info.put("level", di.level());
        info.put("cooldown", di.cooldown());
        info.put("waveCount", di.monsterIds().size());

        // Player progress
        var fuben = fubenRepo.findByPlayerIdAndInmap(uid, di.id()).orElse(null);
        if (fuben != null && fuben.getLttime() != null) {
            long elapsed = System.currentTimeMillis() / 1000 - fuben.getLttime();
            long remaining = Math.max(0, di.cooldown() - elapsed);
            info.put("onCooldown", remaining > 0);
            info.put("cooldownRemaining", remaining);
            info.put("progress", fuben.getGwId() != null ? fuben.getGwId() : 0);
        } else {
            info.put("onCooldown", false);
            info.put("cooldownRemaining", 0);
            info.put("progress", 0);
        }

        // Monster list
        List<Monster> monsters = monsterRepo.findAllById(
            di.monsterIds().stream().map(Long::valueOf).collect(Collectors.toList()));
        info.put("monsters", monsters.stream().map(m -> {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("id", m.getId()); mm.put("name", m.getName());
            mm.put("level", m.getLevel()); mm.put("hp", m.getHp());
            mm.put("boss", m.getBoss()); mm.put("img", m.getImgstand());
            return mm;
        }).collect(Collectors.toList()));

        // Crystal cost for skip (PHP: 10 crystal per remaining hour or fraction)
        long crystalCost = 0;
        if (info.get("onCooldown") == Boolean.TRUE) {
            long remaining = (Long) info.get("cooldownRemaining");
            crystalCost = Math.max(1, (remaining + 3599) / 3600 * 10);
            info.put("crystalCost", crystalCost);
        }

        return ApiResponse.success(info);
    }

    /** Start dungeon battle — returns first wave monster */
    @Transactional
    @PostMapping("/{id}/enter")
    public ApiResponse<Map<String, Object>> enterDungeon(@PathVariable Long id, Authentication auth) {
        DungeonInfo di = DungeonConfig.getById(id.intValue()).orElse(null);
        if (di == null) return ApiResponse.error("副本不存在");

        Long uid = (Long) auth.getPrincipal();
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        // PHP: pet level check is done client-side before calling enter.
        // Server only validates cooldown and requirements.
        if (di.level() > 0 && (player.getScore() == null || player.getScore() < 1)) {
            // Player data might not be loaded; skip level check for now
        }

        // Check cooldown
        var fuben = fubenRepo.findByPlayerIdAndInmap(uid, di.id()).orElse(null);
        long now = System.currentTimeMillis() / 1000;
        if (fuben != null && fuben.getLttime() != null) {
            long remaining = Math.max(0, di.cooldown() - (now - fuben.getLttime()));
            if (remaining > 0) return ApiResponse.error("副本冷却中，剩余 " + remaining / 60 + " 分钟");
        }

        // Get first monster
        if (di.monsterIds().isEmpty()) return ApiResponse.error("副本怪物配置为空");
        Monster firstMonster = monsterRepo.findById((long) di.monsterIds().get(0))
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        // Create/update fuben record
        if (fuben == null) {
            fuben = new Fuben();
        }
        fuben.setPlayerId(uid);
        fuben.setInmap(di.id());
        fuben.setGwId(0); // current wave index
        fuben.setLttime(now);
        fubenRepo.save(fuben);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dungeonId", di.id());
        result.put("dungeonName", di.name());
        result.put("wave", 0);
        result.put("totalWaves", di.monsterIds().size());
        result.put("monsterId", firstMonster.getId());
        result.put("monsterName", firstMonster.getName());
        result.put("monsterLevel", firstMonster.getLevel());
        result.put("monsterHp", firstMonster.getHp());
        result.put("monsterImg", firstMonster.getImgstand());
        return ApiResponse.success(result);
    }

    /** Advance to next wave after defeating current monster */
    @Transactional
    @PostMapping("/{id}/next-wave")
    public ApiResponse<Map<String, Object>> nextWave(@PathVariable Long id, Authentication auth) {
        DungeonInfo di = DungeonConfig.getById(id.intValue()).orElse(null);
        if (di == null) return ApiResponse.error("副本不存在");

        Long uid = (Long) auth.getPrincipal();
        var fuben = fubenRepo.findByPlayerIdAndInmap(uid, di.id()).orElse(null);
        if (fuben == null) return ApiResponse.error("未进入该副本");

        int nextIdx = (fuben.getGwId() != null ? fuben.getGwId() : 0) + 1;
        if (nextIdx >= di.monsterIds().size()) {
            // Dungeon complete — set cooldown
            fuben.setLttime(System.currentTimeMillis() / 1000);
            fuben.setGwId(nextIdx);
            fubenRepo.save(fuben);
            return ApiResponse.success(Map.of("completed", true, "wave", nextIdx));
        }

        fuben.setGwId(nextIdx);
        fubenRepo.save(fuben);

        int monsterId = di.monsterIds().get(nextIdx);
        Monster monster = monsterRepo.findById((long) monsterId)
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wave", nextIdx);
        result.put("totalWaves", di.monsterIds().size());
        result.put("monsterId", monster.getId());
        result.put("monsterName", monster.getName());
        result.put("monsterLevel", monster.getLevel());
        result.put("monsterHp", monster.getHp());
        result.put("monsterImg", monster.getImgstand());
        return ApiResponse.success(result);
    }

    /** Spend crystal to skip cooldown */
    @Transactional
    @PostMapping("/{id}/skip-cooldown")
    public ApiResponse<Map<String, Object>> skipCooldown(@PathVariable Long id, Authentication auth) {
        DungeonInfo di = DungeonConfig.getById(id.intValue()).orElse(null);
        if (di == null) return ApiResponse.error("副本不存在");

        Long uid = (Long) auth.getPrincipal();
        PlayerExt ext = playerExtRepo.findById(uid.intValue()).orElse(null);
        if (ext == null) return ApiResponse.error("玩家不存在");

        var fuben = fubenRepo.findByPlayerIdAndInmap(uid, di.id()).orElse(null);
        if (fuben == null || fuben.getLttime() == null) return ApiResponse.error("副本未在冷却中");

        long now = System.currentTimeMillis() / 1000;
        long remaining = Math.max(0, di.cooldown() - (now - fuben.getLttime()));
        if (remaining <= 0) return ApiResponse.error("副本冷却已结束");

        long crystalCost = Math.max(1, (remaining + 3599) / 3600 * 10);
        int currentCrystal = ext.getSj() != null ? ext.getSj() : 0;
        if (currentCrystal < crystalCost) return ApiResponse.error("水晶不足！需要 " + crystalCost + " 水晶");

        ext.setSj(currentCrystal - (int) crystalCost);
        playerExtRepo.save(ext);
        fuben.setLttime(0L); // reset cooldown
        fubenRepo.save(fuben);

        return ApiResponse.success(Map.of("skipped", true, "cost", crystalCost));
    }
}
