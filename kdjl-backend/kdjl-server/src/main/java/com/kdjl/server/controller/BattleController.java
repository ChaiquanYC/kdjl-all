package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.BattleService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    public record FightRequest(Long petId, Long monsterId, Integer difficulty) {}
    public record ActionRequest(String action, Long skillId, Long bagId) {}

    /** Old batch PvE — kept for backward compat */
    @PostMapping("/pve")
    public ApiResponse<BattleService.BattleResult> fightPve(
            @RequestBody FightRequest req, Authentication auth) {
        Long uidLong = (Long) auth.getPrincipal();
        return ApiResponse.success(battleService.fight(uidLong.intValue(), req.petId(), req.monsterId()));
    }

    /** Start an interactive battle session */
    @PostMapping("/init")
    public ApiResponse<Map<String, Object>> initBattle(
            @RequestBody FightRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int diff = req.difficulty() != null ? req.difficulty() : 1;
        return ApiResponse.success(battleService.initBattle(uid, req.petId(), req.monsterId(), diff));
    }

    public record AutoFightRequest(String mode) {} // "gold" or "yb"

    /** Start/stop auto-fight. Mode: "gold"/"yb"/"stop" */
    @PostMapping("/auto-fight")
    public ApiResponse<Map<String, Object>> autoFight(@RequestBody AutoFightRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(battleService.setAutoFight(uid, req.mode()));
    }

    /** Perform a single action: {"action":"attack"} or {"action":"skill","skillId":123} */
    @PostMapping("/action")
    public ApiResponse<Map<String, Object>> action(
            @RequestBody ActionRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(battleService.performAction(uid, req.action(), req.skillId(), req.bagId()));
    }

    /** Use healing item during battle */
    @PostMapping("/use-item")
    public ApiResponse<Map<String, Object>> useItem(@RequestBody Map<String, Object> body, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Long bagId = Long.valueOf(body.get("bagId").toString());
        return ApiResponse.success(battleService.useBattleItem(uid, bagId));
    }

    /** Phase 2: Monster's turn (auto-called by frontend after delay) */
    @PostMapping("/monster-turn")
    public ApiResponse<Map<String, Object>> monsterTurn(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(battleService.monsterTurn(uid));
    }

    /** Flee from battle */
    @PostMapping("/flee")
    public ApiResponse<Map<String, Object>> flee(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(battleService.fleeBattle(uid));
    }

    /** Get current battle state (for reconnection) */
    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> getState(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Map<String, Object> state = battleService.getBattleState(uid);
        return state != null ? ApiResponse.success(state) : ApiResponse.error("没有进行中的战斗");
    }

    /** Team battle — leader initiates, all active members' main pets participate */
    public record TeamFightRequest(Long monsterId, Integer difficulty) {}
    @PostMapping("/team-fight")
    public ApiResponse<Map<String, Object>> teamFight(@RequestBody TeamFightRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int diff = req.difficulty() != null ? req.difficulty() : 1;
        Map<String, Object> result = battleService.teamFight(uid, req.monsterId(), diff);
        if (result.containsKey("error")) return ApiResponse.error((String) result.get("error"));
        return ApiResponse.success(result);
    }
}
