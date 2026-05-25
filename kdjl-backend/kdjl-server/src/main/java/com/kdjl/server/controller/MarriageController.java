package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.MarriageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marriage")
public class MarriageController {

    private final MarriageService service;

    public MarriageController(MarriageService service) { this.service = service; }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.getMarriageStatus(uid.intValue()));
    }

    public record ProposeRequest(Long targetPlayerId, Long bagItemId, int count) {}

    @PostMapping("/propose")
    public ApiResponse<Map<String, Object>> propose(@RequestBody ProposeRequest req, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.propose(uid.intValue(), req.targetPlayerId(), req.bagItemId(), req.count()));
    }

    @PostMapping("/accept/{proposerId}")
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long proposerId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.acceptMarriage(uid.intValue(), proposerId));
    }

    @PostMapping("/divorce/request")
    public ApiResponse<Map<String, Object>> requestDivorce(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.requestDivorce(uid.intValue()));
    }

    @PostMapping("/divorce/accept")
    public ApiResponse<Map<String, Object>> acceptDivorce(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.acceptDivorce(uid.intValue()));
    }

    @PostMapping("/divorce/cancel")
    public ApiResponse<Map<String, Object>> cancelDivorce(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(service.cancelDivorce(uid.intValue()));
    }
}
