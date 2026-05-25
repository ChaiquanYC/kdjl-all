package com.kdjl.server.battle;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory battle sessions. Auto-expires sessions older than 30 minutes.
 */
@Component
public class BattleSessionManager {

    private final ConcurrentHashMap<String, BattleSession> sessions = new ConcurrentHashMap<>();

    public BattleSession create(Long playerId, Long userPetId, String petName,
                                String petImg, String petHeadImg, String petImgAck, String petImgDie, int petLevel,
                                Long monsterId, String monsterName, String monsterImg,
                                String monsterImgAck, String monsterImgDie,
                                int monsterLevel, Integer monsterWx,
                                long petHp, long petMaxHp, long petMp, long petMaxMp,
                                long monsterHp, long monsterMaxHp) {
        removeByPlayer(playerId);
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        BattleSession session = new BattleSession(sessionId, playerId, userPetId, petName,
            petImg, petHeadImg, petImgAck, petImgDie, petLevel,
            monsterId, monsterName, monsterImg, monsterImgAck, monsterImgDie,
            monsterLevel, monsterWx, petHp, petMaxHp, petMp, petMaxMp, monsterHp, monsterMaxHp);
        sessions.put(sessionId, session);
        return session;
    }

    public BattleSession get(String sessionId) {
        BattleSession s = sessions.get(sessionId);
        if (s == null) return null;
        // Expire after 30 minutes
        if (System.currentTimeMillis() / 1000 - s.getCreatedAt() > 1800) {
            sessions.remove(sessionId);
            return null;
        }
        return s;
    }

    public BattleSession getByPlayer(Long playerId) {
        return sessions.values().stream()
            .filter(s -> s.getPlayerId().equals(playerId))
            .findFirst().orElse(null);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public void removeByPlayer(Long playerId) {
        sessions.values().removeIf(s -> s.getPlayerId().equals(playerId));
    }

    /**
     * Check minimum action interval (anti-speed-hack). Returns true if allowed.
     */
    public boolean checkActionInterval(BattleSession session) {
        long now = System.currentTimeMillis() / 1000;
        return (now - session.getLastActionTime()) >= 0;
    }

    // Cleanup old sessions (can be called by scheduler)
    public void cleanup() {
        long now = System.currentTimeMillis() / 1000;
        sessions.values().removeIf(s -> now - s.getCreatedAt() > 1800);
    }
}
