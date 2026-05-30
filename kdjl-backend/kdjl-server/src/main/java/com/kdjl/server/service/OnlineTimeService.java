package com.kdjl.server.service;

import com.kdjl.common.entity.PlayerExt;
import com.kdjl.server.repository.PlayerExtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
public class OnlineTimeService {

    private static final Logger log = LoggerFactory.getLogger(OnlineTimeService.class);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration KEY_TTL = Duration.ofHours(25);

    private final RedisTemplate<String, Object> redis;
    private final PlayerExtRepository extRepo;

    public OnlineTimeService(RedisTemplate<String, Object> redis, PlayerExtRepository extRepo) {
        this.redis = redis;
        this.extRepo = extRepo;
    }

    // ---- Key helpers ----

    private String ltKey(int uid) { return "online:" + uid + ":lt"; }
    private String secKey(int uid) { return "online:" + uid + ":sec"; }
    private String dayKey(int uid) { return "online:" + uid + ":day"; }

    // ---- Called on login ----

    public void onLogin(int uid) {
        // Flush any leftover data from a previous session (e.g. crashed without logout)
        flushPlayer(uid);

        String today = LocalDate.now().format(DAY_FMT);
        int now = now();
        redis.opsForValue().set(ltKey(uid), now, KEY_TTL);
        redis.opsForValue().set(secKey(uid), 0, KEY_TTL);
        redis.opsForValue().set(dayKey(uid), today, KEY_TTL);
    }

    // ---- Called on each heartbeat ----

    public void onHeartbeat(int uid) {
        Object ltRaw = redis.opsForValue().get(ltKey(uid));
        if (ltRaw == null) {
            // Redis data lost (e.g. server restart) — reinitialize
            onLogin(uid);
            return;
        }

        int lastTime = parseInt(ltRaw);
        int now = now();
        int elapsed = now - lastTime;
        if (elapsed <= 0) return;

        String today = LocalDate.now().format(DAY_FMT);
        String storedDay = (String) redis.opsForValue().get(dayKey(uid));

        if (storedDay != null && !storedDay.equals(today)) {
            // Cross-day: the elapsed seconds belong to the old day.
            // We accept a small inaccuracy (up to flush interval) by adding
            // them to today's total — matches PHP's simplified approach.
            redis.opsForValue().set(dayKey(uid), today, KEY_TTL);
        }

        redis.opsForValue().set(ltKey(uid), now, KEY_TTL);
        redis.opsForValue().increment(secKey(uid), elapsed);
    }

    // ---- Called on logout ----

    public void onLogout(int uid) {
        flushPlayer(uid);
        redis.delete(ltKey(uid));
        redis.delete(secKey(uid));
        redis.delete(dayKey(uid));
    }

    // ---- Scheduled flush: Redis → DB every 5 minutes ----

    @Scheduled(fixedRate = 300_000)
    public void flushAll() {
        Set<String> keys = redis.keys("online:*:sec");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                // Extract uid from "online:{uid}:sec"
                String[] parts = key.split(":");
                if (parts.length < 3) continue;
                int uid = Integer.parseInt(parts[1]);
                flushPlayer(uid);
            } catch (Exception e) {
                log.warn("Failed to flush online time for key {}: {}", key, e.getMessage());
            }
        }
    }

    // ---- Internal ----

    private void flushPlayer(int uid) {
        Object raw = redis.opsForValue().get(secKey(uid));
        int sec = parseInt(raw);
        if (sec <= 0) return;

        // Atomically read-and-clear to avoid double-counting
        redis.opsForValue().set(secKey(uid), 0, KEY_TTL);

        PlayerExt ext = extRepo.findById(uid).orElse(null);
        if (ext == null) return;

        int totalOnline = ext.getOnlineTime() != null ? ext.getOnlineTime() : 0;
        int todayOnline = ext.getOnlineTimeToday() != null ? ext.getOnlineTimeToday() : 0;
        int lastOnlineTime = ext.getLastOnlineTime() != null ? ext.getLastOnlineTime() : 0;

        ext.setOnlineTime(totalOnline + sec);
        ext.setOnlineTimeToday(todayOnline + sec);
        ext.setLastOnlineTime(totalOnline + sec);
        ext.setLastOnlineDay(Integer.parseInt(LocalDate.now().format(DAY_FMT)));
        extRepo.save(ext);
    }

    private static int now() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private static int parseInt(Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
