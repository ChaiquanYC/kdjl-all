package com.kdjl.server.service;

import com.kdjl.common.entity.OnlineRewardConfig;
import com.kdjl.server.repository.OnlineRewardConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OnlineRewardConfigService {

    private final OnlineRewardConfigRepository configRepo;

    // Cached config: step -> (levelMin -> config row)
    private volatile Map<Integer, Map<Integer, OnlineRewardConfig>> configByStepAndLevel = new LinkedHashMap<>();
    private volatile int[] timeThresholds = {10, 30, 60, 120, 300}; // defaults

    // Hardcoded fallback (matches current DailyController)
    private static final int[] DEFAULT_THRESHOLDS = {10, 30, 60, 120, 300};
    private static final Map<Integer, Map<Integer, Map<Long, Integer>>> DEFAULT_REWARDS = buildDefaultRewards();

    public OnlineRewardConfigService(OnlineRewardConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        List<OnlineRewardConfig> all = configRepo.findAllByOrderByStepAscLevelMinAsc();
        if (all.isEmpty()) {
            configByStepAndLevel = new LinkedHashMap<>();
            timeThresholds = DEFAULT_THRESHOLDS;
            return;
        }

        Map<Integer, Map<Integer, OnlineRewardConfig>> byStep = new LinkedHashMap<>();
        Set<Integer> thresholds = new LinkedHashSet<>();
        for (OnlineRewardConfig c : all) {
            byStep.computeIfAbsent(c.getStep(), k -> new LinkedHashMap<>())
                  .put(c.getLevelMin(), c);
            thresholds.add(c.getTimeMinutes());
        }
        configByStepAndLevel = byStep;
        timeThresholds = thresholds.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(timeThresholds);
    }

    public int[] getTimeThresholds() {
        return timeThresholds;
    }

    public int getStepCount() {
        return configByStepAndLevel.isEmpty() ? 5 : configByStepAndLevel.size();
    }

    /**
     * Get rewards for a given pet level and step index (0-based).
     * Returns map of propId -> count.
     */
    public Map<Long, Integer> getRewards(int level, int stepIndex) {
        if (configByStepAndLevel.isEmpty()) {
            // Fallback to defaults
            return DEFAULT_REWARDS.entrySet().stream()
                .filter(e -> level <= e.getKey())
                .findFirst()
                .map(e -> e.getValue().get(stepIndex))
                .orElse(null);
        }

        int step = stepIndex + 1; // DB uses 1-based step
        Map<Integer, OnlineRewardConfig> levelMap = configByStepAndLevel.get(step);
        if (levelMap == null) return null;

        // Find the matching level tier (highest levelMin <= level)
        OnlineRewardConfig match = null;
        for (OnlineRewardConfig c : levelMap.values()) {
            if (level >= c.getLevelMin() && level <= c.getLevelMax()) {
                match = c;
                break;
            }
        }
        if (match == null) return null;

        String[] ids = match.getItemIds().split(",");
        String[] counts = match.getItemCounts().split(",");
        Map<Long, Integer> rewards = new LinkedHashMap<>();
        for (int i = 0; i < ids.length && i < counts.length; i++) {
            try {
                rewards.put(Long.parseLong(ids[i].trim()), Integer.parseInt(counts[i].trim()));
            } catch (NumberFormatException ignored) {}
        }
        return rewards;
    }

    /**
     * Get eligible step (1-based) based on online minutes.
     */
    public int getEligibleStep(int onlineMinutes) {
        for (int i = timeThresholds.length - 1; i >= 0; i--) {
            if (onlineMinutes > timeThresholds[i]) return i + 1;
        }
        return 0;
    }

    private static Map<Integer, Map<Integer, Map<Long, Integer>>> buildDefaultRewards() {
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
}
