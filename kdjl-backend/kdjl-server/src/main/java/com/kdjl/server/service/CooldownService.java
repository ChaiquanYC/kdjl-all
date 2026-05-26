package com.kdjl.server.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player per-operation cooldowns using in-memory timestamps.
 * Matching PHP intervals: evolve=2s, compose=5s, nirvana=10s, sacred-rebirth=30s.
 */
@Service
public class CooldownService {

    private final ConcurrentHashMap<String, Long> lastOpTime = new ConcurrentHashMap<>();

    public enum Op {
        EVOLVE("evolve", 2_000),
        COMPOSE("compose", 5_000),
        NIRVANA("nirvana", 10_000),
        SACRED_EVOLVE("sacredEvolve", 2_000),
        EXTRACT_GROWTH("extractGrowth", 2_000),
        CONVERT_GROWTH("convertGrowth", 2_000),
        SACRED_REBIRTH("sacredRebirth", 30_000);

        final String key;
        final long cooldownMs;

        Op(String key, long cooldownMs) { this.key = key; this.cooldownMs = cooldownMs; }
    }

    /** Returns remaining cooldown ms, or 0 if ready. */
    public long checkCooldown(Long playerId, Op op) {
        String cacheKey = playerId + ":" + op.key;
        Long last = lastOpTime.get(cacheKey);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= op.cooldownMs) return 0;
        return op.cooldownMs - elapsed;
    }

    /** Record that player performed an operation. */
    public void recordOp(Long playerId, Op op) {
        lastOpTime.put(playerId + ":" + op.key, System.currentTimeMillis());
    }
}
