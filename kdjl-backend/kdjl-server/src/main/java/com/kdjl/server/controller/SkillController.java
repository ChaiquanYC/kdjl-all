package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.SkillService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets/{petId}/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listSkills(@PathVariable Long petId) {
        return ApiResponse.success(skillService.getPetSkills(petId));
    }

    @GetMapping("/learnable")
    public ApiResponse<List<Map<String, Object>>> learnableSkills(@PathVariable Long petId) {
        return ApiResponse.success(skillService.getLearnableSkills(petId));
    }

    @PostMapping("/learn/{skillSysId}")
    public ApiResponse<Map<String, Object>> learnSkill(
            Authentication auth,
            @PathVariable Long petId,
            @PathVariable Long skillSysId) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(skillService.learnSkill(uid, petId, skillSysId));
    }

    @PostMapping("/upgrade/{skillId}")
    public ApiResponse<Map<String, Object>> upgradeSkill(
            Authentication auth,
            @PathVariable Long petId,
            @PathVariable Long skillId) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(skillService.upgradeSkill(uid, petId, skillId));
    }
}
