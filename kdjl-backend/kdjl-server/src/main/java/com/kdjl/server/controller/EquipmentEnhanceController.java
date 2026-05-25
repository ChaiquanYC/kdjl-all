package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.EquipmentEnhanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentEnhanceController {

    private final EquipmentEnhanceService service;

    public EquipmentEnhanceController(EquipmentEnhanceService service) {
        this.service = service;
    }

    /** Strengthen equipment (+1 to +15). Body: { bagItemId, auxiliaryBagId? } */
    @PostMapping("/strengthen")
    public ApiResponse<Map<String, Object>> strengthen(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long bagItemId = toLong(body.get("bagItemId"));
        Long auxiliaryBagId = body.get("auxiliaryBagId") != null
            ? toLong(body.get("auxiliaryBagId")) : null;
        try {
            return ApiResponse.success(service.strengthen(uid, bagItemId, auxiliaryBagId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** Decompose equipment into materials. */
    @PostMapping("/decompose/{bagId}")
    public ApiResponse<Map<String, Object>> decompose(
            Authentication auth,
            @PathVariable Long bagId) {
        Long uid = (Long) auth.getPrincipal();
        try {
            return ApiResponse.success(service.decompose(uid, bagId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** Clear gem holes on equipment (洗炼). Body: { equipmentBagId, cleaningItemBagId } */
    @PostMapping("/clear-holes")
    public ApiResponse<Map<String, Object>> clearHoles(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long equipmentBagId = toLong(body.get("equipmentBagId"));
        Long cleaningItemBagId = toLong(body.get("cleaningItemBagId"));
        try {
            return ApiResponse.success(service.clearHoles(uid, equipmentBagId, cleaningItemBagId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long toLong(Object v) {
        if (v == null) throw new IllegalArgumentException("缺少必要参数");
        return Long.valueOf(v.toString());
    }
}
