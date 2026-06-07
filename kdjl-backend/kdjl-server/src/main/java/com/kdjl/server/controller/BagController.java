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

    @GetMapping("/props/all")
    public ApiResponse<List<Map<String, Object>>> getAllProps() {
        return ApiResponse.success(bagService.getAllPropsForClient());
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
        String context = isJs ? "zhanbu" : String.valueOf(body.getOrDefault("context", "bag"));
        return ApiResponse.success(bagService.useItem(uid, id, petId, context));
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
        return ApiResponse.success(bagService.dropItem(uid, id));
    }

    /** Pet evolution (varyname=7, PHP jhGate.php) */
    @PostMapping("/evolve")
    public ApiResponse<Map<String, Object>> evolvePet(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long petId = Long.valueOf(body.get("petId").toString());
        int style = body.get("style") != null ? Integer.parseInt(body.get("style").toString()) : 1;
        Long materialPropId = Long.valueOf(body.get("materialPropId").toString());
        Long protectionItemId = body.get("protectionItemId") != null
            ? Long.valueOf(body.get("protectionItemId").toString()) : null;
        return ApiResponse.success(bagService.handleEvolution(uid, petId, style, materialPropId, protectionItemId));
    }

    /** Pet merge (varyname=8, PHP cmpGate.php) */
    @PostMapping("/merge")
    public ApiResponse<Map<String, Object>> mergePets(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long mainPetId = Long.valueOf(body.get("mainPetId").toString());
        Long subPetId = Long.valueOf(body.get("subPetId").toString());
        Long item1Id = body.get("item1Id") != null ? Long.valueOf(body.get("item1Id").toString()) : null;
        Long item2Id = body.get("item2Id") != null ? Long.valueOf(body.get("item2Id").toString()) : null;
        return ApiResponse.success(bagService.handleMerge(uid, mainPetId, subPetId, item1Id, item2Id));
    }

    /** Equipment strengthening (varyname=10/11, PHP ext_zbstrength.php) */
    @PostMapping("/strengthen")
    public ApiResponse<Map<String, Object>> strengthenEquipment(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long equipBagId = Long.valueOf(body.get("equipBagId").toString());
        Long materialBagId = body.get("materialBagId") != null ? Long.valueOf(body.get("materialBagId").toString()) : null;
        Long auxiliaryBagId = body.get("auxiliaryBagId") != null ? Long.valueOf(body.get("auxiliaryBagId").toString()) : null;
        return ApiResponse.success(bagService.handleStrengthen(uid, equipBagId, materialBagId, auxiliaryBagId));
    }

    /** Gem synthesis (varyname=25+25, PHP xqhcGate.php) */
    @PostMapping("/gem/synthesis")
    public ApiResponse<Map<String, Object>> gemSynthesis(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long gem1BagId = Long.valueOf(body.get("gem1BagId").toString());
        Long gem2BagId = Long.valueOf(body.get("gem2BagId").toString());
        Long protectionBagId = body.get("protectionBagId") != null
            ? Long.valueOf(body.get("protectionBagId").toString()) : null;
        return ApiResponse.success(bagService.handleGemSynthesis(uid, gem1BagId, gem2BagId, protectionBagId));
    }

    /** Gem embedding (varyname=25+9) */
    @PostMapping("/gem/embed")
    public ApiResponse<Map<String, Object>> gemEmbed(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long gemBagId = Long.valueOf(body.get("gemBagId").toString());
        Long equipBagId = Long.valueOf(body.get("equipBagId").toString());
        return ApiResponse.success(bagService.handleGemEmbed(uid, gemBagId, equipBagId));
    }

    /** Gem washing (varyname=26) */
    @PostMapping("/gem/wash")
    public ApiResponse<Map<String, Object>> gemWash(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        Long equipBagId = Long.valueOf(body.get("equipBagId").toString());
        Long washStoneBagId = Long.valueOf(body.get("washStoneBagId").toString());
        return ApiResponse.success(bagService.handleGemWash(uid, equipBagId, washStoneBagId));
    }

    /** Crystal gift (varyname=17, PHP ext_ml.php) */
    @PostMapping("/crystal/give")
    public ApiResponse<Map<String, Object>> giveCrystal(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long uid = (Long) auth.getPrincipal();
        String targetNickname = body.get("targetNickname").toString();
        Long crystalBagId = Long.valueOf(body.get("crystalBagId").toString());
        int count = body.get("count") != null ? Integer.parseInt(body.get("count").toString()) : 1;
        return ApiResponse.success(bagService.handleCrystalGift(uid, targetNickname, crystalBagId, count));
    }
}
