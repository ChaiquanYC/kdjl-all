package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/daily")
public class DailyController {

    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private final UserBagRepository bagRepo;
    private final UserPetRepository petRepo;
    private final PropsRepository propsRepo;

    private static final int[] TIME_THRESHOLDS = {10, 30, 60, 120, 300}; // minutes
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Reward config: levelThreshold -> (stepIndex -> (itemId -> count))
    // Matches PHP onlineforexp config from welcome table
    private static final Map<Integer, Map<Integer, Map<Long, Integer>>> REWARD_CONFIG = buildRewardConfig();

    public DailyController(PlayerRepository playerRepo, PlayerExtRepository playerExtRepo,
                           UserBagRepository bagRepo, UserPetRepository petRepo,
                           PropsRepository propsRepo) {
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.bagRepo = bagRepo;
        this.petRepo = petRepo;
        this.propsRepo = propsRepo;
    }

    private static Map<Integer, Map<Integer, Map<Long, Integer>>> buildRewardConfig() {
        Map<Integer, Map<Integer, Map<Long, Integer>>> config = new LinkedHashMap<>();
        config.put(30, Map.of(
            0, Map.of(2008L, 1, 912L, 2),
            1, Map.of(1994L, 1, 913L, 2),
            2, Map.of(2778L, 1, 913L, 2),
            3, Map.of(2791L, 15, 2794L, 2),
            4, Map.of(2780L, 3, 2810L, 3)));
        config.put(60, Map.of(
            0, Map.of(2008L, 2, 912L, 3),
            1, Map.of(1994L, 2, 913L, 3),
            2, Map.of(2778L, 2, 913L, 3),
            3, Map.of(2791L, 20, 2794L, 3),
            4, Map.of(2780L, 5, 2810L, 5)));
        config.put(Integer.MAX_VALUE, Map.of(
            0, Map.of(2008L, 3, 912L, 4),
            1, Map.of(1994L, 3, 913L, 4),
            2, Map.of(2778L, 3, 913L, 4),
            3, Map.of(2791L, 30, 2794L, 5),
            4, Map.of(2780L, 8, 2810L, 8)));
        return config;
    }

    @PostMapping("/online-reward")
    @Transactional
    public ApiResponse<Map<String, Object>> onlineReward(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int uidInt = uid.intValue();

        Player player = playerRepo.findById(uidInt).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        PlayerExt ext = playerExtRepo.findById(uidInt).orElse(null);
        if (ext == null) {
            ext = new PlayerExt();
            ext.setPlayerId(uidInt);
        }

        UserPet battlePet = getBattlePet(player);
        if (battlePet == null) {
            return ApiResponse.error("请到点击左侧<宠物资料>,再点击一个宠物设置为主战宠物！");
        }

        updateOnlineTime(ext, player);

        int onlineSeconds = ext.getOnlineTimeToday() != null ? ext.getOnlineTimeToday() : 0;
        int onlineMinutes = (int) Math.ceil(onlineSeconds / 60.0);

        int eligibleStep = getEligibleStep(onlineMinutes);
        if (eligibleStep == 0) {
            int remaining = TIME_THRESHOLDS[0] * 60 - onlineSeconds;
            return ApiResponse.error("还不到领奖时间呢！还需在线" + formatTime(remaining));
        }

        int currentStep = ext.getExpGotStep() != null ? ext.getExpGotStep() : 0;
        if (currentStep >= eligibleStep) {
            return ApiResponse.error("你已经领取完毕了！");
        }

        int petLevel = battlePet.getLevel() != null ? battlePet.getLevel() : 1;
        Map<Long, Integer> rewards = getRewardsForLevel(petLevel, currentStep);
        if (rewards == null || rewards.isEmpty()) {
            return ApiResponse.error("后台没有给等级为" + petLevel + "的宠物做设定！");
        }

        if (!hasBagSpace(uid, player, rewards.size())) {
            return ApiResponse.error("您的背包空间不足，请整理后再来领取！");
        }

        StringBuilder prizeWord = new StringBuilder();
        for (Map.Entry<Long, Integer> entry : rewards.entrySet()) {
            giveItem(uid, entry.getKey(), entry.getValue());
            propsRepo.findById(entry.getKey()).ifPresent(p ->
                prizeWord.append(p.getName()).append(" ").append(entry.getValue()).append("个，"));
        }

        ext.setExpGotStep(currentStep + 1);
        playerExtRepo.save(ext);

        boolean isLast = (currentStep + 1 >= 5);
        String message = isLast
            ? "恭喜，您得到了今天最后大奖" + prizeWord + "，今日在线奖励已全部发放，祝您游戏愉快！"
            : "恭喜，您获得在线奖励" + prizeWord + "更大的礼包还在后面，继续努力吧…";

        return ApiResponse.success(Map.of(
            "claimed", true, "step", currentStep + 1, "totalSteps", 5,
            "message", message, "isLast", isLast));
    }

    @GetMapping("/online-reward/check")
    public ApiResponse<Map<String, Object>> checkOnlineReward(Authentication auth) {
        Long uid = (Long) auth.getPrincipal();
        int uidInt = uid.intValue();

        Player player = playerRepo.findById(uidInt).orElse(null);
        if (player == null) return ApiResponse.error("玩家不存在");

        PlayerExt ext = playerExtRepo.findById(uidInt).orElse(null);
        if (ext == null) {
            return ApiResponse.success(Map.of(
                "canClaim", false, "currentStep", 0, "totalSteps", 5,
                "onlineMinutes", 0, "remainingSeconds", TIME_THRESHOLDS[0] * 60,
                "nextThresholdMinutes", TIME_THRESHOLDS[0]));
        }

        updateOnlineTime(ext, player);

        int onlineSeconds = ext.getOnlineTimeToday() != null ? ext.getOnlineTimeToday() : 0;
        int onlineMinutes = (int) Math.ceil(onlineSeconds / 60.0);
        int eligibleStep = getEligibleStep(onlineMinutes);
        int currentStep = ext.getExpGotStep() != null ? ext.getExpGotStep() : 0;
        boolean canClaim = currentStep < eligibleStep;

        int remainingSeconds = 0;
        int nextThresholdMinutes = 0;
        if (currentStep < TIME_THRESHOLDS.length) {
            nextThresholdMinutes = TIME_THRESHOLDS[currentStep];
            remainingSeconds = Math.max(0, nextThresholdMinutes * 60 - onlineSeconds);
        }

        return ApiResponse.success(Map.of(
            "canClaim", canClaim, "currentStep", currentStep, "eligibleStep", eligibleStep,
            "totalSteps", 5, "onlineMinutes", onlineMinutes,
            "remainingSeconds", remainingSeconds, "nextThresholdMinutes", nextThresholdMinutes));
    }

    // --- helpers ---

    private UserPet getBattlePet(Player player) {
        if (player.getMbid() != null) {
            UserPet pet = petRepo.findById(player.getMbid().longValue()).orElse(null);
            if (pet != null) return pet;
        }
        return petRepo.findByPlayerId(player.getId().longValue()).stream().findFirst().orElse(null);
    }

    private void updateOnlineTime(PlayerExt ext, Player player) {
        int now = (int) (System.currentTimeMillis() / 1000);
        int today = Integer.parseInt(LocalDate.now().format(DATE_FMT));

        Integer lastOnlineDay = ext.getLastOnlineDay();

        if (lastOnlineDay == null || lastOnlineDay != today) {
            ext.setExpGotStep(0);
            ext.setLastOnlineDay(today);
            ext.setOnlineTimeToday(0);
            ext.setLastOnlineTime(now);
        } else {
            int lastCheck = ext.getLastOnlineTime() != null ? ext.getLastOnlineTime() : now;
            int delta = now - lastCheck;
            if (delta > 0 && delta < 3600) {
                int onlineToday = ext.getOnlineTimeToday() != null ? ext.getOnlineTimeToday() : 0;
                ext.setOnlineTimeToday(onlineToday + delta);
            }
            ext.setLastOnlineTime(now);
        }
        player.setLastVisitTime(now);
        playerRepo.save(player);
    }

    private static int getEligibleStep(int onlineMinutes) {
        for (int i = TIME_THRESHOLDS.length - 1; i >= 0; i--) {
            if (onlineMinutes > TIME_THRESHOLDS[i]) return i + 1;
        }
        return 0;
    }

    private Map<Long, Integer> getRewardsForLevel(int level, int step) {
        return REWARD_CONFIG.entrySet().stream()
            .filter(e -> level <= e.getKey())
            .findFirst()
            .map(e -> e.getValue().get(step))
            .orElse(null);
    }

    private boolean hasBagSpace(Long uid, Player player, int itemTypes) {
        long count = bagRepo.findByPlayerId(uid).stream()
            .filter(b -> b.getSums() != null && b.getSums() > 0
                && (b.getZbing() == null || b.getZbing() == 0))
            .count();
        int maxBag = player.getMaxBag() != null ? player.getMaxBag() : 50;
        return count + itemTypes < maxBag;
    }

    private void giveItem(Long uid, Long propId, int count) {
        UserBag existing = bagRepo.findByPlayerId(uid).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(propId))
            .findFirst().orElse(null);
        if (existing != null) {
            existing.setSums((existing.getSums() != null ? existing.getSums() : 0) + count);
            bagRepo.save(existing);
        } else {
            UserBag bag = new UserBag();
            bag.setPlayerId(uid);
            bag.setPropId(propId);
            bag.setSums(count);
            bag.setVary(1);
            bag.setStime((long) (System.currentTimeMillis() / 1000));
            bag.setZbing(0);
            bagRepo.save(bag);
        }
    }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds >= 3600) {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
        return (totalSeconds / 60) + "分钟";
    }
}
