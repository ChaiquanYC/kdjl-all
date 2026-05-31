package com.kdjl.admin.controller;

import com.kdjl.admin.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminService adminService;

    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** Dashboard statistics */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.getDashboardStats();
    }

    /** Search players */
    @GetMapping("/players")
    public Map<String, Object> players(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = adminService.searchPlayers(keyword, page, size);
        long total = adminService.countPlayers(keyword);
        return Map.of("list", list, "total", total, "page", page, "size", size);
    }

    /** Player detail */
    @GetMapping("/players/{id}")
    public Map<String, Object> playerDetail(@PathVariable Integer id) {
        Map<String, Object> detail = adminService.getPlayerDetail(id);
        if (detail == null) return Map.of("error", "玩家不存在");
        return detail;
    }

    /** Give item to player */
    public record GiveRequest(Integer playerId, Long propId, int count) {}
    @PostMapping("/give-item")
    public Map<String, Object> giveItem(@RequestBody GiveRequest req) {
        return adminService.giveItem(req.playerId(), req.propId(), req.count());
    }

    /** Give currency */
    public record CurrencyRequest(Integer playerId, String type, int amount) {}
    @PostMapping("/give-currency")
    public Map<String, Object> giveCurrency(@RequestBody CurrencyRequest req) {
        return adminService.giveCurrency(req.playerId(), req.type(), req.amount());
    }

    /** Browse props */
    @GetMapping("/props")
    public Map<String, Object> props(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = adminService.browseProps(keyword, page, size);
        long total = adminService.countProps(keyword);
        return Map.of("list", list, "total", total, "page", page, "size", size);
    }

    /** Browse pet templates */
    @GetMapping("/pets")
    public Map<String, Object> pets(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = adminService.browsePets(keyword, page, size);
        long total = adminService.countPets(keyword);
        return Map.of("list", list, "total", total, "page", page, "size", size);
    }

    /** Player ban/mute actions */
    public record BanRequest(Integer playerId) {}
    @PostMapping("/ban")
    public Map<String, Object> ban(@RequestBody BanRequest req) { return adminService.banPlayer(req.playerId()); }
    @PostMapping("/unban")
    public Map<String, Object> unban(@RequestBody BanRequest req) { return adminService.unbanPlayer(req.playerId()); }

    public record MuteRequest(Integer playerId, Integer minutes) {}
    @PostMapping("/mute")
    public Map<String, Object> mute(@RequestBody MuteRequest req) { return adminService.mutePlayer(req.playerId(), req.minutes() != null ? req.minutes() : 60); }
    @PostMapping("/unmute")
    public Map<String, Object> unmute(@RequestBody BanRequest req) { return adminService.unmutePlayer(req.playerId()); }

    /** Battle logs for player */
    @GetMapping("/fight-logs/{playerId}")
    public List<Map<String, Object>> fightLogs(@PathVariable Integer playerId) {
        return adminService.getFightLogs(playerId);
    }

    /** Payment/yb consumption stats */
    @GetMapping("/payment-stats")
    public Map<String, Object> paymentStats() {
        return adminService.getPaymentStats();
    }

    // ======================== Task Definition CRUD ========================

    /** List/search task definitions */
    @GetMapping("/tasks")
    public Map<String, Object> tasks(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer color,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = adminService.browseTasks(keyword, color, page, size);
        long total = adminService.countTasks(keyword, color);
        return Map.of("list", list, "total", total, "page", page, "size", size);
    }

    /** Get single task definition */
    @GetMapping("/tasks/{id}")
    public Map<String, Object> getTask(@PathVariable Long id) {
        Map<String, Object> task = adminService.getTask(id);
        if (task == null) return Map.of("error", "任务不存在");
        return task;
    }

    /** Create task */
    @PostMapping("/tasks")
    public Map<String, Object> createTask(@RequestBody Map<String, Object> data) {
        return adminService.createTask(data);
    }

    /** Update task */
    @PutMapping("/tasks/{id}")
    public Map<String, Object> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return adminService.updateTask(id, data);
    }

    /** Delete task */
    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        return adminService.deleteTask(id);
    }

    /** Get next available cid and max xulie for a cid */
    @GetMapping("/tasks/next-cid")
    public Map<String, Object> nextCid() {
        return adminService.getNextCid();
    }

    @GetMapping("/tasks/max-xulie")
    public Map<String, Object> maxXulie(@RequestParam String cid) {
        return Map.of("maxXulie", adminService.getMaxXulie(cid));
    }

    // ======================== Item/Monster Quick Search ========================

    /** Quick search items (id+name only, for autocomplete) */
    @GetMapping("/items/search")
    public List<Map<String, Object>> searchItems(@RequestParam(defaultValue = "") String keyword) {
        return adminService.searchItems(keyword);
    }

    /** Batch lookup items by IDs, returns id→name map */
    @GetMapping("/items/lookup")
    public Map<Long, String> lookupItems(@RequestParam String ids) {
        return adminService.lookupItems(ids);
    }

    /** Quick search monsters/pet templates (id+name only) */
    @GetMapping("/monsters/search")
    public List<Map<String, Object>> searchMonsters(@RequestParam(defaultValue = "") String keyword) {
        return adminService.searchMonsters(keyword);
    }

    /** Batch lookup monsters by IDs, returns id→name map */
    @GetMapping("/monsters/lookup")
    public Map<Long, String> lookupMonsters(@RequestParam String ids) {
        return adminService.lookupMonsters(ids);
    }

    // ======================== Player Task Management ========================

    /** Get player's accepted tasks */
    @GetMapping("/tasks/player/{playerId}")
    public List<Map<String, Object>> playerTasks(@PathVariable Integer playerId) {
        return adminService.getPlayerTasks(playerId);
    }

    /** Assign task to player */
    public record AssignTaskRequest(Integer playerId, Long taskId) {}
    @PostMapping("/tasks/assign")
    public Map<String, Object> assignTask(@RequestBody AssignTaskRequest req) {
        return adminService.assignTask(req.playerId(), req.taskId());
    }

    /** Update player's task state */
    public record UpdatePlayerTaskRequest(Integer playerId, Long taskId, String state, String comself) {}
    @PutMapping("/tasks/player")
    public Map<String, Object> updatePlayerTask(@RequestBody UpdatePlayerTaskRequest req) {
        return adminService.updatePlayerTask(req.playerId(), req.taskId(), req.state(), req.comself());
    }

    /** Remove player's task */
    public record RemovePlayerTaskRequest(Integer playerId, Long taskId) {}
    @DeleteMapping("/tasks/player")
    public Map<String, Object> removePlayerTask(@RequestBody RemovePlayerTaskRequest req) {
        return adminService.removePlayerTask(req.playerId(), req.taskId());
    }

    // ======================== Online Reward Config ========================

    @GetMapping("/online-rewards/config")
    public Map<String, Object> onlineRewardConfig() {
        return Map.of("list", adminService.getOnlineRewardConfig());
    }

    @PutMapping("/online-rewards/config")
    public Map<String, Object> saveOnlineRewardConfig(@RequestBody List<Map<String, Object>> configs) {
        return adminService.saveOnlineRewardConfig(configs);
    }

    @GetMapping("/online-rewards/players")
    public Map<String, Object> onlineRewardPlayers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.getOnlineRewardPlayers(page, size);
    }

    public record ResetRewardRequest(Integer playerId) {}
    @PostMapping("/online-rewards/reset-player")
    public Map<String, Object> resetOnlineReward(@RequestBody ResetRewardRequest req) {
        return adminService.resetPlayerOnlineReward(req.playerId());
    }
}
