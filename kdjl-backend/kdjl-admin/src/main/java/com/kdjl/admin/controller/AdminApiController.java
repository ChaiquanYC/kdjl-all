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
}
