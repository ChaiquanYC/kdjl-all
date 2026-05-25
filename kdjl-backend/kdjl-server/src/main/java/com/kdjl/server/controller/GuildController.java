package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.GuildService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/guild")
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) { this.guildService = guildService; }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(guildService.listGuilds());
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myGuild(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        Map<String, Object> g = guildService.getMyGuild(uid.intValue());
        return g != null ? ApiResponse.success(g) : ApiResponse.error("未加入公会");
    }

    @GetMapping("/{guildId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long guildId) {
        return ApiResponse.success(guildService.getGuild(guildId));
    }

    public record CreateRequest(String name, String info) {}

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(guildService.createGuild(uid.intValue(), req.name(), req.info()));
    }

    @PostMapping("/join/{guildId}")
    public ApiResponse<Map<String, Object>> join(@PathVariable Long guildId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(guildService.joinGuild(uid.intValue(), guildId));
    }

    @PostMapping("/leave")
    public ApiResponse<Map<String, Object>> leave(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(guildService.leaveGuild(uid.intValue()));
    }
}
