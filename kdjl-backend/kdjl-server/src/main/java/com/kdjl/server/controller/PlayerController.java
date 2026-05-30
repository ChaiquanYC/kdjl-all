package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.OnlineTimeService;
import com.kdjl.server.service.PlayerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;
    private final OnlineTimeService onlineTimeService;

    public PlayerController(PlayerService playerService, OnlineTimeService onlineTimeService) {
        this.playerService = playerService;
        this.onlineTimeService = onlineTimeService;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth) {
        Long uidLong = (Long) auth.getPrincipal();
        Integer uid = uidLong.intValue();
        playerService.updateOnlineStatus(uid);
        return ApiResponse.success(playerService.getPlayerInfo(uid));
    }

    @PostMapping("/enter-map/{mapId}")
    public ApiResponse<Map<String, Object>> enterMap(@PathVariable Integer mapId, Authentication auth) {
        Long uidLong = (Long) auth.getPrincipal();
        Integer uid = uidLong.intValue();
        playerService.enterMap(uid, mapId);
        return ApiResponse.success(Map.of("inMap", mapId));
    }

    @PostMapping("/leave-map")
    public ApiResponse<Map<String, Object>> leaveMap(Authentication auth) {
        Long uidLong = (Long) auth.getPrincipal();
        playerService.leaveMap(uidLong.intValue());
        return ApiResponse.success(Map.of("inMap", 0));
    }

    @GetMapping("/online/count")
    public ApiResponse<Map<String, Object>> onlineCount() {
        return ApiResponse.success(Map.of("count", playerService.getOnlineCount()));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(Authentication auth) {
        Integer uid = ((Long) auth.getPrincipal()).intValue();
        onlineTimeService.onHeartbeat(uid);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication auth) {
        Integer uid = ((Long) auth.getPrincipal()).intValue();
        onlineTimeService.onLogout(uid);
        return ApiResponse.success(null);
    }
}
