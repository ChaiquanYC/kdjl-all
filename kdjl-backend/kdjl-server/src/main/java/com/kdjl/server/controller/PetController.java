package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.battle.*;
import com.kdjl.server.service.PetService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final BattleSessionManager sessionMgr;

    public PetController(PetService petService, BattleSessionManager sessionMgr) {
        this.petService = petService;
        this.sessionMgr = sessionMgr;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listPets(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.getPlayerPets(uid));
    }

    @GetMapping("/{petId}")
    public ApiResponse<Map<String, Object>> getPet(@PathVariable Long petId, Authentication auth) {
        return ApiResponse.success(petService.getPetDetail(petId));
    }

    @PostMapping("/capture/{monsterId}")
    public ApiResponse<Map<String, Object>> capture(@PathVariable Long monsterId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        BattleSession session = sessionMgr.getByPlayer(uid);
        return ApiResponse.success(petService.capture(uid, monsterId, session));
    }

    @PostMapping("/set-main/{petId}")
    public ApiResponse<Map<String, Object>> setMain(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.setMainPet(uid.intValue(), petId));
    }

    @PostMapping("/heal/{petId}")
    public ApiResponse<Map<String, Object>> heal(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.healPet(uid, petId));
    }

    @PostMapping("/heal-all")
    public ApiResponse<Map<String, Object>> healAll(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.healAllPets(uid));
    }

    @GetMapping("/ranch")
    public ApiResponse<List<Map<String, Object>>> ranchPets(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.getRanchPets(uid));
    }

    @PostMapping("/{petId}/deposit")
    public ApiResponse<Map<String, Object>> deposit(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.depositPet(uid, petId));
    }

    @PostMapping("/{petId}/withdraw")
    public ApiResponse<Map<String, Object>> withdraw(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.withdrawPet(uid, petId));
    }

    @PostMapping("/{petId}/discard")
    public ApiResponse<Map<String, Object>> discard(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(petService.discardPet(uid, petId));
    }
}
