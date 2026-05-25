package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.TeamService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;
    public TeamController(TeamService teamService) { this.teamService = teamService; }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list() { return ApiResponse.success(teamService.listTeams()); }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> my(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Map<String, Object> t = teamService.getMyTeam(uid.intValue());
        return t != null ? ApiResponse.success(t) : ApiResponse.error("未加入队伍");
    }

    public record CreateRequest(String name) {}
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.createTeam(uid.intValue(), req.name()));
    }

    @PostMapping("/join/{teamId}")
    public ApiResponse<Map<String, Object>> join(@PathVariable Long teamId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.joinTeam(uid.intValue(), teamId));
    }

    @PostMapping("/leave")
    public ApiResponse<Map<String, Object>> leave(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.leaveTeam(uid.intValue()));
    }

    /** Kick a member from team (leader only) */
    @PostMapping("/kick/{playerId}")
    public ApiResponse<Map<String, Object>> kick(@PathVariable Long playerId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.kickMember(uid.intValue(), playerId));
    }

    /** Approve a pending member (leader only) */
    @PostMapping("/approve/{playerId}")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long playerId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.approveMember(uid.intValue(), playerId));
    }

    /** Toggle away status */
    @PostMapping("/away")
    public ApiResponse<Map<String, Object>> toggleAway(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(teamService.toggleAway(uid.intValue()));
    }
}
