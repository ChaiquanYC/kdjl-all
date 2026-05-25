package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.Monster;
import com.kdjl.server.repository.MonsterRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/monsters")
public class MonsterController {

    private final MonsterRepository monsterRepo;

    public MonsterController(MonsterRepository monsterRepo) {
        this.monsterRepo = monsterRepo;
    }

    @GetMapping
    public ApiResponse<List<Monster>> listMonsters(
            @RequestParam(defaultValue = "1") int minLevel,
            @RequestParam(defaultValue = "100") int maxLevel) {
        return ApiResponse.success(monsterRepo.findByLevelBetween(minLevel, maxLevel));
    }

    @GetMapping("/{id}")
    public ApiResponse<Monster> getMonster(@PathVariable Long id) {
        Optional<Monster> m = monsterRepo.findById(id);
        return m.map(ApiResponse::success)
            .orElseGet(() -> ApiResponse.error("怪物不存在"));
    }

    @GetMapping("/boss")
    public ApiResponse<List<Monster>> listBosses() {
        return ApiResponse.success(monsterRepo.findByBoss(1));
    }
}
