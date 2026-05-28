package com.kdjl.server.battle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory battle session (replaces PHP $_SESSION['fight'.$uid]).
 * Stores per-round state: pet/monster HP, round count, status.
 */
public class BattleSession {

    public enum State { WAITING, PET_ACT, MONSTER_ACT, ROUND_END, WON, LOST, FLED }

    private final String sessionId;
    private final Long playerId;
    private final Long userPetId;
    private final String petName;
    private final Long monsterId;
    private final String monsterName;
    // Display fields — persisted across state updates
    private final String petImg;
    private final String petHeadImg;
    private final String petImgAck;
    private final String petImgDie;
    private final String monsterImg;
    private final String monsterImgAck;
    private final String monsterImgDie;
    private final int monsterLevel;
    private final Integer monsterWx;
    private final int petLevel;
    private long petHp;
    private long petMaxHp;
    private long petMp;
    private long petMaxMp;
    private long monsterHp;
    private long monsterMaxHp;
    private int round;
    private State state;
    private long lastActionTime;
    private long createdAt;
    private final List<RoundLog> logs = new ArrayList<>();

    // Equipment bonuses applied to pet stats
    private long equipAc, equipMc, equipHits, equipMiss, equipSpeed;

    // Pet EXP for UI display (set at init, updated after battle)
    private long petNowexp;
    private long petLexp;
    private int difficulty = 1;
    // Map type from multiMonsters field: "1"=challenge, "2"=tower, "3"=team, "4"=sacred, ""/"0"=normal
    private String multiMonsters = "";

    // Skill cooldowns: skillId -> cooldownEndTime (unix timestamp)
    private final Map<Long, Long> cooldowns = new ConcurrentHashMap<>();

    public BattleSession(String sessionId, Long playerId, Long userPetId, String petName,
                         String petImg, String petHeadImg, String petImgAck, String petImgDie, int petLevel,
                         Long monsterId, String monsterName, String monsterImg,
                         String monsterImgAck, String monsterImgDie,
                         int monsterLevel, Integer monsterWx,
                         long petHp, long petMaxHp,
                         long petMp, long petMaxMp, long monsterHp, long monsterMaxHp) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.userPetId = userPetId;
        this.petName = petName;
        this.monsterId = monsterId;
        this.monsterName = monsterName;
        this.petImg = petImg;
        this.petHeadImg = petHeadImg;
        this.petImgAck = petImgAck;
        this.petImgDie = petImgDie;
        this.petLevel = petLevel;
        this.monsterImg = monsterImg;
        this.monsterImgAck = monsterImgAck;
        this.monsterImgDie = monsterImgDie;
        this.monsterLevel = monsterLevel;
        this.monsterWx = monsterWx;
        this.petHp = petHp;
        this.petMaxHp = petMaxHp;
        this.petMp = petMp;
        this.petMaxMp = petMaxMp;
        this.monsterHp = monsterHp;
        this.monsterMaxHp = monsterMaxHp;
        this.round = 0;
        this.state = State.WAITING;
        this.lastActionTime = System.currentTimeMillis() / 1000 - 3; // allow immediate first action
        this.createdAt = System.currentTimeMillis() / 1000;
    }

    // ---- Getters ----
    public String getSessionId() { return sessionId; }
    public Long getPlayerId() { return playerId; }
    public Long getUserPetId() { return userPetId; }
    public String getPetName() { return petName; }
    public Long getMonsterId() { return monsterId; }
    public String getMonsterName() { return monsterName; }
    public long getPetHp() { return petHp; }
    public long getPetMaxHp() { return petMaxHp; }
    public long getPetMp() { return petMp; }
    public long getPetMaxMp() { return petMaxMp; }
    public long getMonsterHp() { return monsterHp; }
    public long getMonsterMaxHp() { return monsterMaxHp; }
    public int getRound() { return round; }
    public State getState() { return state; }
    public long getLastActionTime() { return lastActionTime; }
    public long getCreatedAt() { return createdAt; }
    public List<RoundLog> getLogs() { return Collections.unmodifiableList(logs); }
    public Map<Long, Long> getCooldowns() { return Collections.unmodifiableMap(cooldowns); }

    // ---- Setters / Mutators ----
    public void setPetHp(long hp) { this.petHp = Math.max(0, Math.min(petMaxHp, hp)); }
    public void setPetMp(long mp) { this.petMp = Math.max(0, Math.min(petMaxMp, mp)); }
    public void setMonsterHp(long hp) { this.monsterHp = Math.max(0, hp); }
    public void setState(State state) { this.state = state; }
    public void incrementRound() { this.round++; }
    public void touchAction() { this.lastActionTime = System.currentTimeMillis() / 1000; }
    public void addLog(RoundLog log) { this.logs.add(log); }

    public void setCooldown(Long skillId, int seconds) {
        cooldowns.put(skillId, System.currentTimeMillis() / 1000 + seconds);
    }
    public long getEquipAc() { return equipAc; }
    public long getEquipMc() { return equipMc; }
    public long getEquipHits() { return equipHits; }
    public long getEquipMiss() { return equipMiss; }
    public long getEquipSpeed() { return equipSpeed; }
    public void setEquipBonuses(long ac, long mc, long hits, long miss, long speed) {
        this.equipAc = ac; this.equipMc = mc; this.equipHits = hits;
        this.equipMiss = miss; this.equipSpeed = speed;
    }
    public void setPetExp(long nowexp, long lexp) { this.petNowexp = nowexp; this.petLexp = lexp; }
    public long getPetNowexp() { return petNowexp; }
    public long getPetLexp() { return petLexp; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public String getMultiMonsters() { return multiMonsters; }
    public void setMultiMonsters(String multiMonsters) { this.multiMonsters = multiMonsters != null ? multiMonsters : ""; }

    public long getCooldownRemaining(Long skillId) {
        Long end = cooldowns.get(skillId);
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis() / 1000;
        return Math.max(0, remaining);
    }

    public Map<String, Object> toStateMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", sessionId);
        m.put("round", round);
        m.put("state", state.name());
        m.put("petHp", petHp);
        m.put("petMaxHp", petMaxHp);
        m.put("petMp", petMp);
        m.put("petMaxMp", petMaxMp);
        m.put("monsterHp", monsterHp);
        m.put("monsterMaxHp", monsterMaxHp);
        m.put("petName", petName);
        m.put("monsterName", monsterName);
        m.put("petImg", petImg); m.put("petHeadImg", petHeadImg);
        m.put("petImgAck", petImgAck); m.put("petImgDie", petImgDie);
        m.put("monsterImg", monsterImg); m.put("monsterImgAck", monsterImgAck);
        m.put("monsterImgDie", monsterImgDie);
        m.put("monsterLevel", monsterLevel);
        m.put("monsterWx", monsterWx); m.put("petLevel", petLevel);
        m.put("petNowexp", petNowexp); m.put("petLexp", petLexp);
        return m;
    }

    public static class RoundLog {
        public int round;
        public String action;    // "attack", "skill:name", "item:name", "flee"
        public long petDamage;
        public boolean petCrit;
        public boolean petMiss;
        public long petLifeSteal;
        public long petManasteal;
        public long petDamageDeepen;
        public long monsterDamage;
        public boolean monsterMiss;
        public long monsterDamageReduce;
        public boolean monsterDead;
        public boolean petDead;
    }
}
