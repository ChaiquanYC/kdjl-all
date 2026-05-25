package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.server.service.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** List available tasks (not yet accepted) */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listTasks(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.listTasks(uid.intValue()));
    }

    /** List currently accepted tasks */
    @GetMapping("/accepted")
    public ApiResponse<List<Map<String, Object>>> listAccepted(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.listAcceptedTasks(uid.intValue()));
    }

    @PostMapping("/accept/{taskId}")
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long taskId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.acceptTask(uid.intValue(), taskId));
    }

    @PostMapping("/complete/{taskId}")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long taskId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.completeTask(uid.intValue(), taskId));
    }

    @PostMapping("/abandon/{taskId}")
    public ApiResponse<Map<String, Object>> abandon(@PathVariable Long taskId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.abandonTask(uid.intValue(), taskId));
    }

    @PostMapping("/visit/{npcId}")
    public ApiResponse<Map<String, Object>> visitNpc(@PathVariable String npcId, Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        return ApiResponse.success(taskService.visitNpc(uid.intValue(), npcId));
    }
}
