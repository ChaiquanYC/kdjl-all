package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.repository.PlayerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rank")
public class RankController {

    private final PlayerRepository playerRepo;

    public RankController(PlayerRepository playerRepo) { this.playerRepo = playerRepo; }

    @GetMapping("/{type}")
    public ApiResponse<List<Map<String, Object>>> rank(@PathVariable String type,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        List<Object[]> raw = switch (type) {
            case "money" -> playerRepo.findTopByMoney();
            case "level" -> playerRepo.findTopByPetLevel();
            case "prestige" -> playerRepo.findTopByPrestige();
            default -> List.of();
        };
        List<Map<String, Object>> result = new ArrayList<>();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, raw.size());
        for (int i = start; i < end; i++) {
            Object[] row = raw.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", i + 1);
            m.put("playerId", row[0]);
            m.put("nickname", row[1]);
            m.put("value", row[2]);
            result.add(m);
        }
        return ApiResponse.success(result, raw.size(), page, limit);
    }
}
