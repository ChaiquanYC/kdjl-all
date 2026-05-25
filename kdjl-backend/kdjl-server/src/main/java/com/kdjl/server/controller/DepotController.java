package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/depot")
public class DepotController {

    private static final Map<Integer, String> CATEGORY_NAMES = Map.ofEntries(
        Map.entry(0, "普通"), Map.entry(1, "辅助"), Map.entry(2, "增益"), Map.entry(3, "捕捉"),
        Map.entry(4, "收集"), Map.entry(5, "技能书"), Map.entry(6, "卡片"), Map.entry(7, "进化"),
        Map.entry(8, "合体"), Map.entry(9, "装备"), Map.entry(10, "精练"),
        Map.entry(11, "宝箱"), Map.entry(12, "特殊"), Map.entry(13, "功能"),
        Map.entry(14, "宠物卵"), Map.entry(15, "合成"), Map.entry(20, "传承")
    );

    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;

    public DepotController(UserBagRepository bagRepo, PropsRepository propsRepo, PlayerRepository playerRepo) {
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.playerRepo = playerRepo;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listDepot(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        List<UserBag> items = bagRepo.findByPlayerId(uid);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBag i : items) {
            if (i.getBsum() == null || i.getBsum() <= 0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("propId", i.getPropId());
            m.put("count", i.getBsum());
            m.put("sell", i.getSell());
            Props p = i.getPropId() != null ? propsRepo.findById(i.getPropId().longValue()).orElse(null) : null;
            if (p != null) {
                m.put("name", p.getName());
                m.put("img", p.getImg());
                m.put("varyname", p.getVaryname());
                int cat = p.getVaryname() != null ? p.getVaryname() : 0;
                m.put("category", CATEGORY_NAMES.getOrDefault(cat, "其他" + cat));
            }
            result.add(m);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/deposit/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> deposit(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        UserBag item = bagRepo.findById(id).orElse(null);
        if (item == null || !item.getPlayerId().equals(uid))
            return ApiResponse.error("物品不存在");

        int current = item.getSums() != null ? item.getSums() : 0;
        int toMove = Math.min(count, current);
        if (toMove <= 0) return ApiResponse.error("数量不足");

        // PHP: check warehouse capacity by distinct item types
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        int maxBase = player != null && player.getMaxBase() != null ? player.getMaxBase() : 50;
        long depotItemTypes = bagRepo.findByPlayerId(uid).stream()
            .filter(i -> i.getBsum() != null && i.getBsum() > 0 && (i.getZbing() == null || i.getZbing() == 0))
            .count();
        // If this item is new to warehouse (bsum was 0), it counts as +1 type
        boolean isNewToDepot = item.getBsum() == null || item.getBsum() == 0;
        if (isNewToDepot && depotItemTypes + 1 > maxBase)
            return ApiResponse.error("仓库空间不足，请先整理仓库");

        int bsum = item.getBsum() != null ? item.getBsum() : 0;
        int newSums = current - toMove;
        int newBsum = bsum + toMove;
        item.setSums(newSums);
        item.setBsum(newBsum);
        bagRepo.save(item);

        return ApiResponse.success(Map.of("deposited", toMove));
    }

    @PostMapping("/withdraw/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> withdraw(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        UserBag item = bagRepo.findById(id).orElse(null);
        if (item == null || !item.getPlayerId().equals(uid))
            return ApiResponse.error("物品不存在");

        int bsum = item.getBsum() != null ? item.getBsum() : 0;
        int toMove = Math.min(count, bsum);
        if (toMove <= 0) return ApiResponse.error("数量不足");

        // PHP: check bag capacity by distinct item types
        Player player = playerRepo.findById(uid.intValue()).orElse(null);
        int maxBag = player != null && player.getMaxBag() != null ? player.getMaxBag() : 30;
        long bagItemTypes = bagRepo.findByPlayerId(uid).stream()
            .filter(i -> i.getSums() != null && i.getSums() > 0 && (i.getZbing() == null || i.getZbing() == 0))
            .count();
        // If this item is new to bag (sums was 0), it counts as +1 type
        boolean isNewToBag = item.getSums() == null || item.getSums() == 0;
        if (isNewToBag && bagItemTypes + 1 > maxBag)
            return ApiResponse.error("背包空间不足，请先整理背包");

        int current = item.getSums() != null ? item.getSums() : 0;
        int newBsum = bsum - toMove;
        int newSums = current + toMove;
        item.setBsum(newBsum);
        item.setSums(newSums);
        bagRepo.save(item);

        return ApiResponse.success(Map.of("withdrawn", toMove));
    }
}
