package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.BagService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bag")
public class BagController {

    private final BagService bagService;

    public BagController(BagService bagService) {
        this.bagService = bagService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listBag(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(bagService.getPlayerBag(uid));
    }

    @GetMapping("/equipment")
    public ApiResponse<List<Map<String, Object>>> listEquipment(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(bagService.getEquipment(uid));
    }

    @PostMapping("/use/{id}")
    public ApiResponse<Map<String, Object>> useItem(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long petId = body.get("petId") != null
            ? Long.valueOf(body.get("petId").toString()) : null;
        boolean isJs = body.get("js") != null && "true".equals(String.valueOf(body.get("js")));
        return ApiResponse.success(bagService.useItem(uid, id, petId, isJs));
    }

    /** Magic house: use item by prop ID (PHP usedProps.php?pid=X&js) */
    @PostMapping("/use-by-pid")
    public ApiResponse<Map<String, Object>> useItemByPid(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long pid = Long.valueOf(body.get("pid").toString());
        boolean isJs = body.get("js") != null && "true".equals(String.valueOf(body.get("js")));
        return ApiResponse.success(bagService.useItemByPid(uid, pid, isJs));
    }

    /** Get all magic stone types (PHP getBagOfVary.php) */
    @GetMapping("/stone-types")
    public ApiResponse<List<Map<String, Object>>> getStoneTypes() {
        return ApiResponse.success(bagService.getMagicStoneTypes());
    }

    @PostMapping("/equip/{id}")
    public ApiResponse<Map<String, Object>> equipItem(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long petId = Long.valueOf(body.get("petId").toString());
        return ApiResponse.success(bagService.equipItem(uid, id, petId));
    }

    @PostMapping("/unequip/{id}")
    public ApiResponse<Map<String, Object>> unequipItem(
            Authentication auth,
            @PathVariable Long id) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(bagService.unequipItem(uid, id));
    }

    @PostMapping("/sell/{id}")
    public ApiResponse<Map<String, Object>> sellItem(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        return ApiResponse.success(bagService.sellItem(uid, id, count));
    }

    @PostMapping("/drop/{id}")
    public ApiResponse<Map<String, Object>> dropItem(
            Authentication auth,
            @PathVariable Long id) {
        Long uid = (Long) auth.getPrincipal();
        bagService.dropItem(uid, id);
        return ApiResponse.success(Map.of("dropped", true));
    }
}
