package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.InheritanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/inherit")
public class InheritanceController {

    private final InheritanceService service;

    public InheritanceController(InheritanceService service) { this.service = service; }

    @GetMapping("/available")
    public ApiResponse<List<Map<String, Object>>> listAvailable(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.listAvailable(uid));
    }

    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> getMyInheritance(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.getMyInheritance(uid));
    }

    @PostMapping("/join/{petId}")
    public ApiResponse<Map<String, Object>> join(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.joinInherit(uid, petId));
    }

    @PostMapping("/cancel/{petId}")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long petId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.cancelInherit(uid, petId));
    }

    @PostMapping("/pair/{myPetId}/{otherPetId}")
    public ApiResponse<Map<String, Object>> pair(@PathVariable Long myPetId, @PathVariable Long otherPetId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.pairPets(uid, myPetId, otherPetId));
    }

    public record BreedRequest(Long pearlBagId, Long skillBagId) {}

    @PostMapping("/breed/{petId}")
    public ApiResponse<Map<String, Object>> breed(@PathVariable Long petId, @RequestBody(required = false) BreedRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.startBreeding(uid, petId,
            req != null ? req.pearlBagId() : null,
            req != null ? req.skillBagId() : null));
    }

    @PostMapping("/complete/{petId}")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long petId,
            @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        boolean useCrystals = body != null && Boolean.TRUE.equals(body.get("useCrystals"));
        return ApiResponse.success(service.completeInherit(uid, petId, useCrystals));
    }
}
