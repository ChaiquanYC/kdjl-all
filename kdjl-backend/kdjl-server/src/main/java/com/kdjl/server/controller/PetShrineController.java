package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.PetShrineService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pet")
public class PetShrineController {

    private final PetShrineService petShrineService;

    public PetShrineController(PetShrineService petShrineService) {
        this.petShrineService = petShrineService;
    }

    /** Tab 1: 进化 */
    @PostMapping("/{id}/evolve")
    public ApiResponse<Map<String, Object>> evolve(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int style = body.get("style") != null ? Integer.parseInt(body.get("style").toString()) : 1;
        Long keepCzlItemId = body.get("keepCzlItemId") != null
            ? Long.valueOf(body.get("keepCzlItemId").toString()) : null;
        return ApiResponse.success(petShrineService.evolve(uid, id, style, keepCzlItemId));
    }

    /** Tab 2: 合成 */
    @PostMapping("/compose")
    public ApiResponse<Map<String, Object>> compose(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long mainPetId = Long.valueOf(body.get("mainPetId").toString());
        Long subPetId = Long.valueOf(body.get("subPetId").toString());
        Long item1Id = body.get("item1Id") != null
            ? Long.valueOf(body.get("item1Id").toString()) : null;
        Long item2Id = body.get("item2Id") != null
            ? Long.valueOf(body.get("item2Id").toString()) : null;
        return ApiResponse.success(petShrineService.compose(uid, mainPetId, subPetId, item1Id, item2Id));
    }

    /** Tab 3: 涅磐 */
    @PostMapping("/nirvana")
    public ApiResponse<Map<String, Object>> nirvana(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long mainPetId = Long.valueOf(body.get("mainPetId").toString());
        Long subPetId = Long.valueOf(body.get("subPetId").toString());
        Long beastId = Long.valueOf(body.get("beastId").toString());
        Long item1Id = body.get("item1Id") != null
            ? Long.valueOf(body.get("item1Id").toString()) : null;
        Long item2Id = body.get("item2Id") != null
            ? Long.valueOf(body.get("item2Id").toString()) : null;
        return ApiResponse.success(petShrineService.nirvana(uid, mainPetId, subPetId, beastId, item1Id, item2Id));
    }

    /** Tab 4: 神圣进化 */
    @PostMapping("/{id}/sacred-evolve")
    public ApiResponse<Map<String, Object>> sacredEvolve(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long itemId = body.get("itemId") != null
            ? Long.valueOf(body.get("itemId").toString()) : null;
        return ApiResponse.success(petShrineService.sacredEvolve(uid, id, itemId));
    }

    /** Tab 4: 成长抽取 */
    @PostMapping("/{id}/extract-growth")
    public ApiResponse<Map<String, Object>> extractGrowth(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long item1Id = body.get("item1Id") != null
            ? Long.valueOf(body.get("item1Id").toString()) : null;
        Long item2Id = body.get("item2Id") != null
            ? Long.valueOf(body.get("item2Id").toString()) : null;
        return ApiResponse.success(petShrineService.extractGrowth(uid, id, item1Id, item2Id));
    }

    /** Tab 4: 成长转化 */
    @PostMapping("/{id}/convert-growth")
    public ApiResponse<Map<String, Object>> convertGrowth(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        int value = Integer.parseInt(body.get("value").toString());
        return ApiResponse.success(petShrineService.convertGrowth(uid, id, value));
    }

    /** Tab 5: 转生目标图片列表 (op=img) */
    @GetMapping("/rebirth-images")
    public ApiResponse<java.util.List<Map<String, Object>>> rebirthImages(
            @RequestParam Long petId) {
        return ApiResponse.success(petShrineService.getRebirthImages(petId));
    }

    /** Tab 5: 转生需求信息 (op=str) */
    @GetMapping("/{id}/rebirth-info")
    public ApiResponse<Map<String, Object>> rebirthInfo(
            @PathVariable Long id,
            @RequestParam Integer zsId) {
        return ApiResponse.success(petShrineService.getRebirthInfo(zsId, id));
    }

    /** Tab 5: 执行神圣转生 (op=zs) */
    @PostMapping("/{id}/sacred-rebirth")
    public ApiResponse<Map<String, Object>> sacredRebirth(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Integer zsId = Integer.valueOf(body.get("zsId").toString());
        Long item1Id = body.get("item1Id") != null
            ? Long.valueOf(body.get("item1Id").toString()) : null;
        Long item2Id = body.get("item2Id") != null
            ? Long.valueOf(body.get("item2Id").toString()) : null;
        return ApiResponse.success(petShrineService.sacredRebirth(uid, id, zsId, item1Id, item2Id));
    }
}
