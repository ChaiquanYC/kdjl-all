package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.config.DungeonConfig;
import com.kdjl.server.repository.*;
import com.kdjl.server.battle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class BattleService {

    private static final Logger log = LoggerFactory.getLogger(BattleService.class);
    private static final int MAX_BATTLE_ROUNDS = 50;

    private final UserPetRepository userPetRepo;
    private final MonsterRepository monsterRepo;
    private final SkillRepository skillRepo;
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;
    private final PetRepository petRepo;
    private final BattleSessionManager sessionMgr;
    private final BagService bagService;
    private final LevelUpService levelUpService;
    private final EquipEffectService equipEffectService;
    private final SkillSysRepository skillSysRepo;

    private final TeamMembersRepository teamMemberRepo;
    private final TeamRepository teamRepo;
    private final GameMapRepository gameMapRepo;
    private final TaskService taskService;
    private final FubenRepository fubenRepo;

    public BattleService(UserPetRepository userPetRepo, MonsterRepository monsterRepo,
                         SkillRepository skillRepo, UserBagRepository bagRepo,
                         PropsRepository propsRepo, PlayerRepository playerRepo,
                         PetRepository petRepo, BattleSessionManager sessionMgr,
                         BagService bagService, LevelUpService levelUpService,
                         EquipEffectService equipEffectService,
                         SkillSysRepository skillSysRepo,
                         TeamMembersRepository teamMemberRepo, TeamRepository teamRepo,
                         GameMapRepository gameMapRepo,
                         TaskService taskService,
                         FubenRepository fubenRepo) {
        this.userPetRepo = userPetRepo;
        this.monsterRepo = monsterRepo;
        this.skillRepo = skillRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.playerRepo = playerRepo;
        this.petRepo = petRepo;
        this.sessionMgr = sessionMgr;
        this.bagService = bagService;
        this.levelUpService = levelUpService;
        this.teamMemberRepo = teamMemberRepo;
        this.teamRepo = teamRepo;
        this.gameMapRepo = gameMapRepo;
        this.equipEffectService = equipEffectService;
        this.skillSysRepo = skillSysRepo;
        this.taskService = taskService;
        this.fubenRepo = fubenRepo;
    }

    private final ThreadLocalRandom rng = ThreadLocalRandom.current();

    /**
     * Execute a single round of combat between a player's pet and a monster.
     */
    @Transactional
    public BattleResult fight(Integer playerId, Long userPetId, Long monsterId) {
        UserPet pet = userPetRepo.findById(userPetId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (pet.getPlayerId() == null || !pet.getPlayerId().equals(playerId.longValue())) {
            throw new IllegalArgumentException("不是你的宠物");
        }

        Monster monster = monsterRepo.findById(monsterId)
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        List<Skill> skills = skillRepo.findByPetId(userPetId);

        BattleResult result = new BattleResult();
        result.petName = pet.getName();
        result.monsterName = monster.getName();
        result.petHp = pet.getHp();
        result.monsterHp = monster.getHp();
        result.rounds = new ArrayList<>();

        long petCurrentHp = pet.getHp() != null ? pet.getHp() : 100;
        long petMaxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
        long monsterCurrentHp = monster.getHp() != null ? monster.getHp() : 100;
        long monsterMaxHp = monster.getHp() != null ? monster.getHp() : 100;

        for (int round = 0; round < MAX_BATTLE_ROUNDS; round++) {
            BattleRound r = new BattleRound();
            r.round = round + 1;

            // --- Pet attacks monster ---
            Skill skill = selectSkill(skills);
            r.petSkill = skill != null ? skill.getName() : "普通攻击";

            boolean hit = rollHit(pet.getHits(), monster.getMiss());
            if (hit) {
                long dmg = calcDamage(pet.getAc(), pet.getMc(), monster.getMc(),
                    skill, pet.getWx(), monster.getWx(), monsterCurrentHp == monsterMaxHp);
                monsterCurrentHp = Math.max(0, monsterCurrentHp - dmg);
                r.petDamage = dmg;
                r.petCrit = rng.nextInt(100) < 5;
                long totalDmg = r.petCrit ? r.petDamage * 2 : r.petDamage;
                monsterCurrentHp = Math.max(0, monsterCurrentHp - totalDmg);

                // Lifesteal check
                r.petLifeSteal = calcLifeSteal(skill, r.petDamage);
                petCurrentHp = Math.min(petMaxHp, petCurrentHp + r.petLifeSteal);
            } else {
                r.petDamage = 0;
                r.missed = true;
            }

            // --- Check monster death ---
            if (monsterCurrentHp <= 0) {
                r.monsterDead = true;
                result.rounds.add(r);
                result.won = true;
                break;
            }

            // --- Monster attacks pet ---
            hit = rollHit(monster.getHits(), pet.getMiss());
            if (hit) {
                long dmg = calcMonsterDamage(monster, pet.getMc() != null ? pet.getMc() : 0, pet.getMiss() != null ? pet.getMiss() : 0);
                petCurrentHp = Math.max(0, petCurrentHp - dmg);
                r.monsterDamage = dmg;
            } else {
                r.monsterDamage = 0;
            }

            // --- Check pet death ---
            if (petCurrentHp <= 0) {
                r.petDead = true;
                result.rounds.add(r);
                result.won = false;
                break;
            }

            result.rounds.add(r);
        }

        // Final state
        result.petHpFinal = petCurrentHp;
        result.monsterHpFinal = monsterCurrentHp;

        if (result.won) {
            result.expGained = calcExpReward(monster, pet.getLevel());
            result.moneyGained = monster.getMoney() != null ? monster.getMoney() : 0;
            result.drops = parseDropList(monster.getDroplist());

            // Apply rewards
            applyRewards(playerId, userPetId, result);
        }

        // Update pet HP
        petCurrentHp = Math.max(1, petCurrentHp); // pet survives with at least 1 HP
        userPetRepo.save(pet); // JPA will track changes

        return result;
    }

    private long recalc(long current, long dmg, boolean crit) {
        long total = crit ? dmg * 2 : dmg;
        return Math.max(0, current - total);
    }

    private Skill selectSkill(List<Skill> skills) {
        if (skills.isEmpty()) return null;
        // Prefer higher level skills with some randomness
        return skills.get(rng.nextInt(skills.size()));
    }

    /**
     * Hit rate calculation from PHP Ack.getHitsP():
     *   hitRate = (attackerHits - defenderMiss) / 100
     *   clamped to [0.1, 1.5]
     */
    private boolean rollHit(Long hits, Long miss) {
        double h = hits != null ? hits : 100;
        double m = miss != null ? miss : 0;
        double rate = (h - m) / 100.0;
        rate = Math.max(0.1, Math.min(1.5, rate));
        return rng.nextDouble() < rate;
    }

    /**
     * Damage formula from PHP Ack.getSkillAck():
     *   base = (petAc + skillAckValue) * skillPlus - monsterMc
     *   base = max(1, base)
     *   damage = round(base * hitRate) + 1
     *   float: random(-10%, +5%)
     *   crit: ×2
     */
    private long calcDamage(Long petAc, Long petMc, Long monsterMc,
                            Skill skill, Integer petElement, Integer monsterElement, boolean isFirstHit) {
        long ac = petAc != null ? petAc : 10;
        long mc = monsterMc != null ? monsterMc : 0;

        long ackValue = 0;
        double plus = 1.0;

        if (skill != null && skill.getValue() != null) {
            try {
                String[] vals = skill.getValue().split(",");
                ackValue = Long.parseLong(vals[0].replaceAll("[^0-9]", "0").isEmpty() ? "0" : vals[0].replaceAll("[^\\d]", ""));
                if (skill.getPlus() != null) {
                    String p = skill.getPlus().split(",")[0].replace("%", "");
                    plus = Double.parseDouble(p.isEmpty() ? "0" : p) / 100.0 + 1.0;
                }
            } catch (NumberFormatException e) {
                ackValue = 0;
                plus = 1.0;
            }
        }

        long base = (long) ((ac + ackValue) * plus - mc);
        base = Math.max(1, base);

        // Element advantage: 金克木 木克土 土克水 水克火 火克金
        double elementMult = calcElementMultiplier(petElement, monsterElement);

        double hitRate = 1.0; // hit already determined, use base 1.0 for damage
        long damage = (long) (base * hitRate * elementMult) + 1;

        // Random float: -10% to +5%
        double floatRange = rng.nextDouble(-0.10, 0.05);
        damage = (long) (damage * (1.0 + floatRange));

        return Math.max(1, damage);
    }

    private long calcMonsterDamage(Monster monster, long petMc, long petMiss) {
        long ac = monster.getAc() != null ? monster.getAc() : 10;
        long base = ac - petMc;
        base = Math.max(1, base);

        long hits = monster.getHits() != null ? monster.getHits() : 100;
        double hitRate = (hits - petMiss) / 100.0;
        hitRate = Math.max(0.1, Math.min(1.5, hitRate));

        long damage = (long) (base * hitRate) + 1;
        double floatRange = rng.nextDouble(-0.10, 0.05);
        damage = (long) (damage * (1.0 + floatRange));

        return Math.max(1, damage);
    }

    /**
     * 五行 element advantage multiplier.
     * 金(1)克木(2) 木(2)克土(5) 土(5)克水(3) 水(3)克火(4) 火(4)克金(1)
     */
    private double calcElementMultiplier(Integer attacker, Integer defender) {
        if (attacker == null || defender == null) return 1.0;
        // attacker beats defender → 1.5x
        // defender beats attacker → 0.7x
        if ((attacker == 1 && defender == 2) || // 金克木
            (attacker == 2 && defender == 5) || // 木克土
            (attacker == 5 && defender == 3) || // 土克水
            (attacker == 3 && defender == 4) || // 水克火
            (attacker == 4 && defender == 1)) { // 火克金
            return 1.5;
        }
        if ((defender == 1 && attacker == 2) ||
            (defender == 2 && attacker == 5) ||
            (defender == 5 && attacker == 3) ||
            (defender == 3 && attacker == 4) ||
            (defender == 4 && attacker == 1)) {
            return 0.7;
        }
        return 1.0;
    }

    private long calcLifeSteal(Skill skill, long damage) {
        // Life steal: if skill has hitshp effect, absorb percentage of damage
        return 0; // Simplified — expand based on skill.img effects
    }

    private long calcExpReward(Monster monster, Integer petLevel) {
        long base = monster.getExps() != null ? monster.getExps() : 10;
        int levelDiff = (monster.getLevel() != null ? monster.getLevel() : 1) - (petLevel != null ? petLevel : 1);
        double mult = 1.0 + levelDiff * 0.05;
        return Math.max(1, (long) (base * Math.max(0.5, Math.min(2.0, mult))));
    }

    /** Parse droplist "propId:prob,propId:prob" — each is independent 1-in-N roll */
    private List<Map<String, Object>> parseDropList(String droplist) {
        if (droplist == null || droplist.isEmpty()) return List.of();
        List<Map<String, Object>> drops = new ArrayList<>();
        for (String part : droplist.split(",")) {
            String[] kv = part.split(":");
            if (kv.length >= 2) {
                try {
                    long propId = Long.parseLong(kv[0].trim());
                    int probability = Integer.parseInt(kv[1].trim());
                    // PHP: rand(1, probability) <= 1 → 1-in-N chance
                    if (rng.nextInt(probability) < 1) {
                        Props p = propsRepo.findById(propId).orElse(null);
                        var drop = new java.util.LinkedHashMap<String, Object>();
                        drop.put("propId", propId);
                        drop.put("name", p != null ? p.getName() : "未知道具");
                        drop.put("count", 1); // Each winning roll drops 1
                        drops.add(drop);
                    }
                } catch (NumberFormatException e) { /* skip malformed entry */ }
            }
        }
        return drops;
    }

    private void applyRewards(Integer playerId, Long petId, BattleResult result) {
        // Update pet exp
        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet != null) {
            long newExp = (pet.getNowexp() != null ? pet.getNowexp() : 0) + result.expGained;
            pet = userPetRepo.save(pet); // in real impl, update exp field and check level up
        }

        // Update player money
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player != null && result.moneyGained > 0) {
            player = playerRepo.save(player); // update money
        }
    }

    // --- Result DTOs ---

    public static class BattleResult {
        public String petName;
        public String monsterName;
        public long petHp;
        public long monsterHp;
        public long petHpFinal;
        public long monsterHpFinal;
        public boolean won;
        public long expGained;
        public long moneyGained;
        public List<Map<String, Object>> drops;
        public List<BattleRound> rounds;
    }

    public static class BattleRound {
        public int round;
        public String petSkill;
        public long petDamage;
        public boolean petCrit;
        public boolean missed;
        public long petLifeSteal;
        public long monsterDamage;
        public boolean petDead;
        public boolean monsterDead;
    }

    // ================================================================
    // Single-round interactive battle (replaces PHP FightGate.php flow)
    // ================================================================

    /**
     * Initialize a new battle session. Called once when player enters battle.
     */
    @Transactional
    public Map<String, Object> initBattle(Long playerId, Long userPetId, Long monsterId, int difficulty, Integer mapId) {
        UserPet pet = userPetRepo.findById(userPetId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("不是你的宠物");
        }
        Monster monster = monsterRepo.findById(monsterId)
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        // PHP Fight_Mod.php:516-521 — read multiMonsters from current map
        // Session flag mapping matches PHP: "1"=challenge, "2"=tower, others=normal
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        String mapMultiMonsters = "";
        if (player != null && player.getInMap() != null) {
            GameMap currentMap = gameMapRepo.findById(player.getInMap()).orElse(null);
            if (currentMap != null) {
                mapMultiMonsters = currentMap.getMultiMonsters() != null ? currentMap.getMultiMonsters() : "";
                // PHP fbfight_Mod.php:50-56 / Fight_Mod.php:617 — sacred map check
                if ("4".equals(mapMultiMonsters)) {
                    if (pet.getWx() == null || pet.getWx() != 7) {
                        throw new IllegalArgumentException("只有神圣宠物,才可以在这里战斗！");
                    }
                }
            }
        }

        long petMaxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
        long petCurrentHp = pet.getHp() != null ? pet.getHp() : petMaxHp;
        long petMaxMp = (pet.getSrcmp() != null ? pet.getSrcmp() : 100) + (pet.getAddmp() != null ? pet.getAddmp() : 0);
        long petCurrentMp = pet.getMp() != null ? pet.getMp() : petMaxMp;
        long monsterMaxHp = monster.getHp() != null ? monster.getHp() : 100;
        long monsterCurrentHp = monsterMaxHp;

        BattleSession session = sessionMgr.create(playerId, userPetId, pet.getName(),
            pet.getImgstand(), pet.getHeadimg(),
            pet.getImgack(), pet.getImgdie(),
            pet.getLevel() != null ? pet.getLevel() : 1,
            monsterId, monster.getName(), monster.getImgstand(),
            monster.getImgack(), monster.getImgdie(),
            monster.getLevel() != null ? monster.getLevel() : 1, monster.getWx(),
            petCurrentHp, petMaxHp, petCurrentMp, petMaxMp,
            monsterCurrentHp, monsterMaxHp);

        // Apply equipment bonuses to session
        var bonuses = bagService.getEquipmentBonuses(userPetId);
        session.setEquipBonuses(
            bonuses.getOrDefault("ac", 0L), bonuses.getOrDefault("mc", 0L),
            bonuses.getOrDefault("hits", 0L), bonuses.getOrDefault("miss", 0L),
            bonuses.getOrDefault("speed", 0L));
        // Store map type (matches PHP $_SESSION['multi_monsters'] flag)
        session.setMultiMonsters(mapMultiMonsters);
        session.setMapId(mapId);
        // Set difficulty
        session.setDifficulty(difficulty);
        // Set EXP for UI display
        session.setPetExp(pet.getNowexp() != null ? pet.getNowexp() : 0,
                          pet.getLexp() != null ? pet.getLexp() : 100);

        Map<String, Object> result = session.toStateMap();
        // Add pet skills for UI
        List<Skill> skills = skillRepo.findByPetId(userPetId);
        List<Map<String, Object>> skillList = new ArrayList<>();
        for (Skill s : skills) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.getId());
            sm.put("name", s.getName());
            sm.put("level", s.getLevel());
            sm.put("uhp", s.getUhp());
            sm.put("ump", s.getUmp());
            skillList.add(sm);
        }
        result.put("skills", skillList);
        result.put("message", "战斗开始！野生 " + monster.getName() + " 出现了！");
        return result;
    }

    /**
     * Team battle — all active members' main pets fight together.
     * PHP: Fight_Mod.php team fight flow. Returns combined results.
     */
    @Transactional
    public Map<String, Object> teamFight(Long leaderPlayerId, Long monsterId, int difficulty) {
        // 1. Verify leader is in a team and is the creator
        var leaderTeams = teamMemberRepo.findByPlayerId(leaderPlayerId);
        if (leaderTeams.isEmpty()) return Map.of("error", "你不在队伍中！");
        TeamMembers leaderMember = leaderTeams.get(0);
        Team team = teamRepo.findById(leaderMember.getTeamId()).orElse(null);
        if (team == null) return Map.of("error", "队伍不存在！");
        if (!team.getCreator().equals(leaderPlayerId))
            return Map.of("error", "只有队长才能发起团队战斗！");

        // 2. Collect all active (state=1) team members
        List<TeamMembers> allMembers = teamMemberRepo.findByTeamId(team.getId());
        List<TeamMembers> activeMembers = allMembers.stream()
            .filter(m -> m.getState() != null && m.getState() == 1)
            .collect(Collectors.toList());
        if (activeMembers.isEmpty()) return Map.of("error", "没有活跃的队员！");

        // 3. Get each member's main pet
        List<UserPet> teamPets = new ArrayList<>();
        List<Player> teamPlayers = new ArrayList<>();
        for (TeamMembers tm : activeMembers) {
            Player p = playerRepo.findById(tm.getPlayerId().intValue()).orElse(null);
            if (p == null || p.getMbid() == null) continue;
            UserPet pet = userPetRepo.findById((long) p.getMbid()).orElse(null);
            if (pet != null) {
                teamPets.add(pet);
                teamPlayers.add(p);
            }
        }
        if (teamPets.isEmpty()) return Map.of("error", "没有队员设置了主战宠物！");

        // 4. Get monster
        Monster monster = monsterRepo.findById(monsterId)
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        // 5. Run cooperative battle: each pet attacks, monster attacks each pet
        List<Map<String, Object>> rounds = new ArrayList<>();
        long monsterHp = monster.getHp() != null ? monster.getHp() : 100;
        long monsterMc = monster.getMc() != null ? monster.getMc() : 10;
        long monsterAc = monster.getAc() != null ? monster.getAc() : 10;
        int monsterLevel = monster.getLevel() != null ? monster.getLevel() : 1;
        boolean won = false;

        for (int round = 1; round <= MAX_BATTLE_ROUNDS && monsterHp > 0; round++) {
            Map<String, Object> roundLog = new LinkedHashMap<>();
            roundLog.put("round", round);
            List<Map<String, Object>> attacks = new ArrayList<>();

            // Each living pet attacks
            for (UserPet pet : teamPets) {
                long petHp = pet.getHp() != null ? pet.getHp() : 1;
                if (petHp <= 0) continue;
                long petAc = pet.getAc() != null ? pet.getAc() : 5;
                long petMc = pet.getMc() != null ? pet.getMc() : 5;
                long baseDmg = Math.max(petAc, petMc);
                long dmg = Math.max(1, baseDmg - monsterMc / 4 + ThreadLocalRandom.current().nextInt(-3, 4));
                monsterHp = Math.max(0, monsterHp - dmg);
                Map<String, Object> atk = new LinkedHashMap<>();
                atk.put("petId", pet.getId()); atk.put("petName", pet.getName());
                atk.put("damage", dmg); atk.put("monsterHpRemain", monsterHp);
                attacks.add(atk);
                if (monsterHp <= 0) break;
            }
            roundLog.put("petAttacks", attacks);

            // Monster counter-attacks
            if (monsterHp > 0) {
                List<Map<String, Object>> monsterHits = new ArrayList<>();
                for (UserPet pet : teamPets) {
                    long petHp = pet.getHp() != null ? pet.getHp() : 1;
                    if (petHp <= 0) continue;
                    long dmg = Math.max(1, monsterAc + ThreadLocalRandom.current().nextInt(-2, 3));
                    pet.setHp(Math.max(0, petHp - dmg));
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("petId", pet.getId()); hit.put("petName", pet.getName());
                    hit.put("damage", dmg); hit.put("petHpRemain", pet.getHp());
                    monsterHits.add(hit);
                }
                roundLog.put("monsterHits", monsterHits);
            }
            rounds.add(roundLog);
            if (monsterHp <= 0) {
                won = true;
                // Track monster kill for each team member's task progress
                for (Player tp : teamPlayers) {
                    taskService.onMonsterKilled(tp.getId(), monster.getId());
                }
                break;
            }
            // Check if all pets dead
            boolean allDead = teamPets.stream().allMatch(p -> (p.getHp() != null ? p.getHp() : 0) <= 0);
            if (allDead) break;
        }

        // 6. Apply rewards to each surviving pet
        double diffMult = difficulty == 3 ? 1.6 : difficulty == 2 ? 1.3 : 1.0;
        double moneyMult = difficulty == 3 ? 1.4 : difficulty == 2 ? 1.2 : 1.0;
        long baseExp = calcExpReward(monster, monsterLevel);
        long teamExp = (long)(baseExp * diffMult / activeMembers.size());
        long teamMoney = (long)((monster.getMoney() != null ? monster.getMoney() : 0) * moneyMult / activeMembers.size());

        List<Map<String, Object>> rewards = new ArrayList<>();
        for (int i = 0; i < teamPets.size(); i++) {
            UserPet pet = teamPets.get(i);
            Player player = i < teamPlayers.size() ? teamPlayers.get(i) : null;
            Map<String, Object> reward = new LinkedHashMap<>();
            reward.put("petId", pet.getId()); reward.put("petName", pet.getName());
            long petHp = pet.getHp() != null ? pet.getHp() : 0;
            if (won && petHp > 0) {
                double expMult = levelUpService.getExpMultiplier(player);
                long finalExp = (long)(teamExp * expMult);
                boolean leveled = levelUpService.addExp(pet, finalExp);
                reward.put("expGained", finalExp);
                reward.put("leveledUp", leveled);
                if (player != null && teamMoney > 0) {
                    int current = player.getMoney() != null ? player.getMoney() : 0;
                    player.setMoney(current + (int) teamMoney);
                    playerRepo.save(player);
                    reward.put("moneyGained", teamMoney);
                }
            } else {
                reward.put("expGained", 0);
                reward.put("moneyGained", 0);
            }
            userPetRepo.save(pet);
            rewards.add(reward);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("won", won);
        result.put("teamId", team.getId());
        result.put("monsterName", monster.getName());
        result.put("rounds", rounds);
        result.put("rewards", rewards);
        result.put("playerCount", teamPets.size());
        return result;
    }

    /**
     * Phase 1: Pet attacks monster. Returns PET_ACT state, frontend then calls monsterTurn after delay.
     */
    @Transactional
    public Map<String, Object> performAction(Long playerId, String actionType, Long skillId, Long bagId) {
        BattleSession session = sessionMgr.getByPlayer(playerId);
        if (session == null) throw new IllegalArgumentException("没有进行中的战斗");

        if (!sessionMgr.checkActionInterval(session)) {
            throw new IllegalArgumentException("操作太快，请等待冷却");
        }
        session.touchAction();

        if (session.getState() == BattleSession.State.WON ||
            session.getState() == BattleSession.State.LOST ||
            session.getState() == BattleSession.State.PET_ACT) {
            throw new IllegalArgumentException("请等待怪物行动");
        }

        Monster monster = monsterRepo.findById(session.getMonsterId())
            .orElseThrow(() -> new IllegalArgumentException("怪物数据异常"));
        UserPet pet = userPetRepo.findById(session.getUserPetId())
            .orElseThrow(() -> new IllegalArgumentException("宠物数据异常"));

        // --- Capture action: try to catch the monster ---
        if ("capture".equals(actionType)) {
            return handleCaptureAction(playerId, session, monster, pet, bagId);
        }

        Skill skill = null;
        if ("skill".equals(actionType) && skillId != null) {
            long cdRemaining = session.getCooldownRemaining(skillId);
            if (cdRemaining > 0) {
                throw new IllegalArgumentException("技能冷却中，剩余" + cdRemaining + "秒");
            }
            skill = skillRepo.findById(skillId).orElse(null);
            if (skill == null) throw new IllegalArgumentException("技能不存在");
            if (skill.getUhp() != null && skill.getUhp() > 0 && session.getPetHp() <= skill.getUhp()) {
                throw new IllegalArgumentException("HP不足");
            }
            if (skill.getUmp() != null && skill.getUmp() > 0 && session.getPetMp() < skill.getUmp()) {
                throw new IllegalArgumentException("MP不足");
            }
        }

        session.incrementRound();
        BattleSession.RoundLog logEntry = new BattleSession.RoundLog();
        logEntry.round = session.getRound();
        logEntry.action = skill != null ? "skill:" + skill.getName() : "attack";

        // Compute equipment special effects for this round
        EquipEffectService.Effects allEff = equipEffectService.parseAllEffects(session.getUserPetId());
        equipEffectService.resolvePercentages(allEff, pet);
        EquipEffectService.BattleEffects battleEff = equipEffectService.computeBattleEffects(allEff, 0, 0); // computed after damage known

        // --- Pet attacks monster (with equipment bonuses) ---
        long petTotalHits = (pet.getHits() != null ? pet.getHits() : 100) - session.getEquipHits() + allEff.hits;
        boolean hit = rollHit(petTotalHits, monster.getMiss());
        long petDmg = 0;
        if (hit) {
            long petTotalAc = (pet.getAc() != null ? pet.getAc() : 10) - session.getEquipAc() + allEff.ac;
            long petTotalMc = (pet.getMc() != null ? pet.getMc() : 10) - session.getEquipMc() + allEff.mc;
            long baseDmg = calcDamage(petTotalAc, petTotalMc, monster.getMc(),
                skill, pet.getWx(), monster.getWx(), session.getMonsterHp() == session.getMonsterMaxHp());

            // Crit check with equipment crit rate
            int totalCritRate = 5 + allEff.crit; // base 5% + equip crit
            boolean crit = rng.nextInt(100) < totalCritRate;
            long dmg = crit ? baseDmg * 2 : baseDmg;

            // Damage deepening (shjs)
            long deepen = Math.round(allEff.shjs * dmg * 0.01);
            dmg += deepen;
            logEntry.petDamageDeepen = deepen;

            // Lifesteal (hitshp)
            long lifesteal = Math.round(allEff.hitshp * dmg * 0.01);
            logEntry.petLifeSteal = lifesteal;
            if (lifesteal > 0) {
                session.setPetHp(session.getPetHp() + lifesteal);
            }

            // Manasteal (hitsmp)
            long manasteal = Math.round(allEff.hitsmp * dmg * 0.01);
            if (manasteal > 0) {
                session.setPetMp(session.getPetMp() + manasteal);
            }

            session.setMonsterHp(session.getMonsterHp() - dmg);
            petDmg = dmg;
            logEntry.petCrit = crit;
        } else {
            logEntry.petMiss = true;
        }
        logEntry.petDamage = petDmg;

        // Deduct HP/MP cost
        if (skill != null) {
            if (skill.getUhp() != null && skill.getUhp() > 0) session.setPetHp(session.getPetHp() - skill.getUhp());
            if (skill.getUmp() != null && skill.getUmp() > 0) session.setPetMp(session.getPetMp() - skill.getUmp());
            if (skill.getSkillDefId() != null) {
                long sid = skill.getSkillDefId();
                if (sid == 319 || sid == 320) session.setCooldown(skill.getId(), 299);
                else if (sid == 321 || sid == 322) session.setCooldown(skill.getId(), 179);
                else if (sid == 323) session.setCooldown(skill.getId(), 119);
            }
        }

        // Check monster death after pet attack
        if (session.getMonsterHp() <= 0) {
            logEntry.monsterDead = true;
            session.setState(BattleSession.State.WON);
            // Track monster kill for task progress
            taskService.onMonsterKilled(playerId.intValue(), monster.getId());
            // Auto-advance dungeon progress on kill (matches PHP fbfightGate.php)
            advanceDungeonProgress(session);
            session.addLog(logEntry);
            Map<String, Object> result = session.toStateMap();
            result.put("phase", "pet");
            result.put("log", logEntry);
            long baseExp = calcExpReward(monster, pet.getLevel());
            // Apply difficulty multiplier for display
            double diffMult = session.getDifficulty() == 3 ? 1.6 : session.getDifficulty() == 2 ? 1.3 : 1.0;
            long moneyRaw = monster.getMoney() != null ? monster.getMoney() : 0;
            double moneyMult = session.getDifficulty() == 3 ? 1.4 : session.getDifficulty() == 2 ? 1.2 : 1.0;
            result.put("won", true);
            result.put("expGained", (long)(baseExp * diffMult));
            result.put("moneyGained", (long)(moneyRaw * moneyMult));
            result.put("drops", parseDropList(monster.getDroplist()));

            // Apply rewards (level-up handled inside)
            boolean leveledUp = applyRewardsOnce(playerId.intValue(), session, monster);
            // Always read back updated pet EXP
            UserPet updated = userPetRepo.findById(session.getUserPetId()).orElse(null);
            if (updated != null) {
                result.put("petNowexp", updated.getNowexp() != null ? updated.getNowexp() : 0);
                result.put("petLexp", updated.getLexp() != null ? updated.getLexp() : 100);
                if (leveledUp) {
                    result.put("levelUp", true);
                    result.put("newLevel", updated.getLevel());
                }
            }
            sessionMgr.remove(session.getSessionId());
            return result;
        }

        session.addLog(logEntry);
        session.setState(BattleSession.State.PET_ACT); // waiting for monster turn

        Map<String, Object> result = session.toStateMap();
        result.put("phase", "monster_turn");
        result.put("log", logEntry);
        return result;
    }

    /**
     * Handle capture action — use 精灵球 to catch the monster.
     * Catch rate based on remaining HP%: lower HP → higher success chance.
     */
    private Map<String, Object> handleCaptureAction(Long playerId, BattleSession session,
                                                     Monster monster, UserPet pet, Long bagId) {
        // Use the specific capture item the player selected
        UserBag ball = null;
        if (bagId != null) {
            ball = bagRepo.findById(bagId).orElse(null);
        }
        if (ball == null || !ball.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("捕捉道具不存在");
        }

        // Consume one ball
        if (ball.getSums() != null && ball.getSums() > 1) {
            ball.setSums(ball.getSums() - 1);
            bagRepo.save(ball);
        } else {
            bagRepo.delete(ball);
        }

        // Parse capture ball effect (PHP format: catch:id1|id2:rate%:flag)
        Props ballProps = propsRepo.findById(ball.getPropId()).orElse(null);
        double catchRate = 0;
        boolean targetsMatch = false;
        if (ballProps != null && ballProps.getEffect() != null) {
            for (String part : ballProps.getEffect().split(",")) {
                String[] kv = part.trim().split(":");
                if (kv.length < 2 || !kv[0].contains("catch")) continue;
                // Format: catch:monsterId1|monsterId2:rate%:flag
                String[] targetIds = kv[1].split("\\|");
                for (String tid : targetIds) {
                    try {
                        if (Long.parseLong(tid.trim()) == monster.getId()) {
                            targetsMatch = true;
                            break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (targetsMatch && kv.length >= 3) {
                    // Parse rate: "50%" → 0.50
                    String rateStr = kv[2].replace("%", "").trim();
                    try { catchRate = Double.parseDouble(rateStr) / 100.0; }
                    catch (NumberFormatException ignored) {}
                }
                break; // only first catch effect matters
            }
        }

        if (!targetsMatch) {
            // Ball doesn't match this monster — reject
            session.addLog(null); // no-op for consistency
            Map<String, Object> result = session.toStateMap();
            result.put("message", "这个精灵球不能捕捉 " + monster.getName() + "！");
            return result;
        }

        if (catchRate <= 0) catchRate = 0.5; // fallback 50%

        boolean caught = rng.nextDouble() < catchRate;

        session.incrementRound();
        BattleSession.RoundLog logEntry = new BattleSession.RoundLog();
        logEntry.round = session.getRound();
        logEntry.action = "capture";

        if (caught) {
            logEntry.monsterDead = true;
            session.setState(BattleSession.State.WON);
            // Track monster kill for task progress
            taskService.onMonsterKilled(playerId.intValue(), monster.getId());
            // Auto-advance dungeon progress on kill (matches PHP fbfightGate.php)
            advanceDungeonProgress(session);
            session.addLog(logEntry);

            // Create new pet from captured monster — use bb template for images/stats
            UserPet newPet = new UserPet();
            newPet.setPlayerId(playerId);
            newPet.setLevel(monster.getLevel() != null ? monster.getLevel() : 1);
            newPet.setWx(monster.getWx());

            // Look up pet template (bb) from monster's catchid for proper images
            Pet template = null;
            if (monster.getCatchItemId() != null) {
                template = petRepo.findById(monster.getCatchItemId()).orElse(null);
            }
            if (template != null) {
                newPet.setName(template.getName());
                newPet.setImgstand(template.getImgstand());
                newPet.setImgack(template.getImgack());
                newPet.setImgdie(template.getImgdie());
                newPet.setHeadimg(template.getHeadimg());
                newPet.setCardimg(template.getCardimg());
                newPet.setKx(template.getKx());
                newPet.setSkillList(template.getSkillList());
                newPet.setHp(template.getHp()); newPet.setMp(template.getMp());
                newPet.setSrchp(template.getHp()); newPet.setSrcmp(template.getMp());
                newPet.setAc(template.getAc()); newPet.setMc(template.getMc());
                newPet.setHits(template.getHits().longValue()); newPet.setMiss(template.getMiss().longValue());
                newPet.setSpeed(template.getSpeed().longValue());
                newPet.setSubyl(template.getSubyl()); newPet.setSubsl(template.getSubsl());
                newPet.setSubxl(template.getSubxl()); newPet.setSubdl(template.getSubdl());
                newPet.setSubfl(template.getSubfl()); newPet.setSubhl(template.getSubhl());
                newPet.setSubkl(template.getSubkl());
                if (template.getCzl() != null && template.getCzl().contains(",")) {
                    String[] czlRange = template.getCzl().replace(".","").split(",");
                    int minCzl = Integer.parseInt(czlRange[0]), maxCzl = Integer.parseInt(czlRange[1]);
                    newPet.setCzl(String.valueOf((minCzl + rng.nextInt(maxCzl - minCzl + 1)) / 10.0));
                } else {
                    newPet.setCzl(template.getCzl() != null ? template.getCzl() : "1.0");
                }
            } else {
                // Fallback: use monster data directly
                newPet.setName(monster.getName());
                newPet.setImgstand(monster.getImgstand());
                newPet.setHp(monster.getHp() != null ? monster.getHp() : 100L);
                newPet.setMp(monster.getMp() != null ? monster.getMp() : 50L);
                newPet.setSrchp(monster.getHp() != null ? monster.getHp() : 100L);
                newPet.setSrcmp(monster.getMp() != null ? monster.getMp() : 50L);
                newPet.setAc(monster.getAc()); newPet.setMc(monster.getMc());
                newPet.setHits(monster.getHits()); newPet.setMiss(monster.getMiss());
                newPet.setSpeed(monster.getSpeed());
                newPet.setKx(monster.getKx());
            }
            // PHP: if carried slots full, send to ranch; if ranch also full, reject
            long carriedCount = userPetRepo.findByPlayerId(playerId).stream()
                .filter(p -> p.getMuchang() == null || p.getMuchang() == 0).count();
            if (carriedCount >= 3) {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                int maxMc = player != null && player.getMaxMc() != null ? player.getMaxMc() : 10;
                long ranchCount = userPetRepo.findByPlayerId(playerId).stream()
                    .filter(p -> p.getMuchang() != null && p.getMuchang() == 1).count();
                if (ranchCount >= maxMc) {
                    throw new IllegalArgumentException("携带和牧场都已满，无法捕捉！请先整理空间");
                }
                newPet.setMuchang(1); // auto-deposit to ranch
            }

            newPet.setNowexp(0L);
            newPet.setLexp(100L);
            UserPet saved = userPetRepo.save(newPet);

            // Create initial skills from template skillist (format: "skillId:level,skillId:level")
            if (template != null && template.getSkillList() != null && !template.getSkillList().isEmpty()) {
                for (String skEntry : template.getSkillList().split(",")) {
                    String[] parts = skEntry.split(":");
                    if (parts.length < 2) continue;
                    try {
                        long skillSysId = Long.parseLong(parts[0].trim());
                        SkillSys wp = skillSysRepo.findById(skillSysId).orElse(null);
                        if (wp == null) continue;
                        Skill skill = new Skill();
                        skill.setPetId(saved.getId());
                        skill.setSkillDefId(skillSysId);
                        skill.setName(wp.getName());
                        skill.setLevel(1);
                        skill.setWx(wp.getWx());
                        skill.setVary(wp.getVary());
                        String[] ack = wp.getAckvalue() != null ? wp.getAckvalue().split(",") : new String[]{"0"};
                        String[] plus = wp.getPlus() != null ? wp.getPlus().split(",") : new String[]{""};
                        String[] uhpArr = wp.getUhp() != null ? wp.getUhp().split(",") : new String[]{"0"};
                        String[] umpArr = wp.getUmp() != null ? wp.getUmp().split(",") : new String[]{"0"};
                        skill.setValue(ack.length > 0 ? ack[0] : "0");
                        skill.setPlus(plus.length > 0 ? plus[0] : "");
                        skill.setUhp(uhpArr.length > 0 ? Integer.parseInt(uhpArr[0].replaceAll("[^0-9\\-]", "0")) : 0);
                        skill.setUmp(umpArr.length > 0 ? Integer.parseInt(umpArr[0].replaceAll("[^0-9\\-]", "0")) : 0);
                        skill.setImg("0");
                        skillRepo.save(skill);
                    } catch (NumberFormatException ignored) {}
                }
            }

            Map<String, Object> result = session.toStateMap();
            result.put("phase", "pet");
            result.put("log", logEntry);
            result.put("won", true);
            result.put("captureSuccess", true);
            result.put("capturedPetId", saved.getId());
            result.put("message", "成功捕捉 " + monster.getName() + "！");
            sessionMgr.remove(session.getSessionId());
            return result;
        } else {
            // Capture failed — monster gets a counter-attack
            logEntry.petMiss = true;
            session.addLog(logEntry);
            session.setState(BattleSession.State.PET_ACT);
            Map<String, Object> result = session.toStateMap();
            result.put("phase", "monster_turn");
            result.put("log", logEntry);
            result.put("message", "捕捉失败！" + monster.getName() + " 挣脱了精灵球！");
            return result;
        }
    }

    /**
     * Phase 2: Monster counter-attacks. Called by frontend after delay.
     */
    @Transactional
    public Map<String, Object> monsterTurn(Long playerId) {
        BattleSession session = sessionMgr.getByPlayer(playerId);
        if (session == null) throw new IllegalArgumentException("没有进行中的战斗");
        if (session.getState() != BattleSession.State.PET_ACT) {
            throw new IllegalArgumentException("宠物还未行动");
        }

        Monster monster = monsterRepo.findById(session.getMonsterId())
            .orElseThrow(() -> new IllegalArgumentException("怪物数据异常"));
        UserPet pet = userPetRepo.findById(session.getUserPetId())
            .orElseThrow(() -> new IllegalArgumentException("宠物数据异常"));

        // Get the last log entry and update it with monster damage
        List<BattleSession.RoundLog> logs = session.getLogs();
        BattleSession.RoundLog logEntry = logs.get(logs.size() - 1);

        // --- Monster attacks pet (with equipment damage reduction) ---
        EquipEffectService.Effects allEff = equipEffectService.parseAllEffects(session.getUserPetId());
        equipEffectService.resolvePercentages(allEff, pet);
        long petTotalMiss = (pet.getMiss() != null ? pet.getMiss() : 0) - session.getEquipMiss() + allEff.miss;
        boolean hit = rollHit(monster.getHits(), petTotalMiss);
        long monsterDmg = 0;
        if (hit) {
            long petTotalMcForDef = (pet.getMc() != null ? pet.getMc() : 0) - session.getEquipMc() + allEff.mc;
            monsterDmg = calcMonsterDamage(monster, petTotalMcForDef, petTotalMiss);
            // Damage reduction (dxsh) — capped at 70%
            long reduce = Math.round(allEff.dxsh * monsterDmg * 0.01);
            long actualDmg = Math.max(1, monsterDmg - reduce);
            logEntry.monsterDamageReduce = reduce;
            session.setPetHp(session.getPetHp() - actualDmg);
            if (reduce > 0) logEntry.monsterDamage = monsterDmg; // store original for display
            else logEntry.monsterDamage = actualDmg;
        } else {
            logEntry.monsterMiss = true;
            logEntry.monsterDamage = 0;
        }

        // Check pet death
        if (session.getPetHp() <= 0) {
            logEntry.petDead = true;
            session.setState(BattleSession.State.LOST);
            Map<String, Object> result = session.toStateMap();
            result.put("phase", "monster");
            result.put("log", logEntry);
            result.put("won", false);
            sessionMgr.remove(session.getSessionId());
            return result;
        }

        session.setState(BattleSession.State.WAITING);
        Map<String, Object> result = session.toStateMap();
        result.put("phase", "monster");
        result.put("log", logEntry);
        return result;
    }

    /** Start/stop auto-fight. Matches PHP ext_Fight.php + Fight_Mod.php line 676/718 */
    @Transactional
    public Map<String, Object> setAutoFight(Long playerId, String mode) {
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null) throw new IllegalArgumentException("玩家不存在");

        Map<String, Object> result = new java.util.LinkedHashMap<>();

        if (!"stop".equals(mode)) {
            // PHP: auto-fight only allowed on normal maps (multi_monsters != "1" && != "2")
            BattleSession session = sessionMgr.getByPlayer(playerId);
            if (session != null) {
                String mm = session.getMultiMonsters();
                if ("1".equals(mm) || "2".equals(mm)) {
                    result.put("message", "挑战地图和通天塔无法使用自动战斗！");
                    result.put("autoFight", false);
                    return result;
                }
            }
        }

        if ("stop".equals(mode)) {
            player.setAutoFitFlag(0);
            playerRepo.save(player);
            result.put("message", "已关闭自动战斗");
            result.put("autoFight", false);
            return result;
        }

        if ("gold".equals(mode)) {
            int remaining = player.getSysAutoSum() != null ? player.getSysAutoSum() : 0;
            if (remaining <= 0) {
                result.put("message", "金币版自动战斗次数不足！剩余次数：0");
                result.put("autoFight", false);
                result.put("remaining", 0);
                return result;
            }
            player.setAutoFitFlag(1);
            playerRepo.save(player);
            result.put("message", "开启金币版自动战斗成功！剩余次数：" + remaining + " 次 (1.2倍经验)");
            result.put("autoFight", true);
            result.put("mode", "gold");
            result.put("remaining", remaining);
            result.put("expMult", 1.2);
            return result;
        }

        if ("yb".equals(mode)) {
            int remaining = player.getMaxAutoFitSum() != null ? player.getMaxAutoFitSum() : 0;
            if (remaining <= 0) {
                result.put("message", "元宝版自动战斗次数不足！剩余次数：0");
                result.put("autoFight", false);
                result.put("remaining", 0);
                return result;
            }
            player.setAutoFitFlag(1);
            playerRepo.save(player);
            result.put("message", "开启元宝版自动战斗成功！剩余次数：" + remaining + " 次 (1.5倍经验)");
            result.put("autoFight", true);
            result.put("mode", "yb");
            result.put("remaining", remaining);
            result.put("expMult", 1.5);
            return result;
        }

        throw new IllegalArgumentException("未知模式: " + mode);
    }

    /** Use a healing item during battle. Matches PHP getProps.php */
    @Transactional
    public Map<String, Object> useBattleItem(Long playerId, Long bagId) {
        BattleSession session = sessionMgr.getByPlayer(playerId);
        if (session == null) throw new IllegalArgumentException("没有进行中的战斗");

        if (session.getState() == BattleSession.State.WON ||
            session.getState() == BattleSession.State.LOST) {
            throw new IllegalArgumentException("战斗已结束");
        }

        UserBag item = bagRepo.findById(bagId).orElse(null);
        if (item == null || !item.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("道具不存在");
        }

        Props props = propsRepo.findById(item.getPropId()).orElse(null);
        if (props == null || props.getEffect() == null) {
            throw new IllegalArgumentException("道具无效");
        }

        // Parse effect: "hp:500,mp:200"
        long hpGain = 0, mpGain = 0;
        for (String part : props.getEffect().split(",")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            try {
                long val = Long.parseLong(kv[0].trim().replaceAll("[^0-9]", "0").isEmpty() ? "0" : kv[0].trim().replaceAll("[^\\d]", ""));
                if (kv[0].contains("hp")) hpGain = Long.parseLong(kv[1].trim());
                else if (kv[0].contains("mp")) mpGain = Long.parseLong(kv[1].trim());
            } catch (NumberFormatException ignored) {}
        }

        if (hpGain == 0 && mpGain == 0) {
            throw new IllegalArgumentException("此道具不能用于战斗");
        }

        // Apply healing
        long newHp = Math.min(session.getPetMaxHp(), session.getPetHp() + hpGain);
        long newMp = Math.min(session.getPetMaxMp(), session.getPetMp() + mpGain);
        session.setPetHp(newHp);
        session.setPetMp(newMp);

        // Consume item
        int remaining = item.getSums() != null ? item.getSums() : 0;
        if (remaining <= 1) bagRepo.delete(item);
        else { item.setSums(remaining - 1); bagRepo.save(item); }

        Map<String, Object> result = session.toStateMap();
        result.put("message", "使用成功！HP+" + hpGain + " MP+" + mpGain);
        result.put("hpGain", hpGain);
        result.put("mpGain", mpGain);
        return result;
    }

    /** Flee from battle */
    public Map<String, Object> fleeBattle(Long playerId) {
        BattleSession session = sessionMgr.getByPlayer(playerId);
        if (session == null) throw new IllegalArgumentException("没有进行中的战斗");
        session.setState(BattleSession.State.FLED);
        Map<String, Object> result = session.toStateMap();
        result.put("message", "你逃跑了！");
        sessionMgr.remove(session.getSessionId());
        return result;
    }

    /** Get current battle state (for reconnection) */
    public Map<String, Object> getBattleState(Long playerId) {
        BattleSession session = sessionMgr.getByPlayer(playerId);
        if (session == null) return null;
        return session.toStateMap();
    }

    private boolean applyRewardsOnce(Integer playerId, BattleSession session, Monster monster) {
        boolean leveledUp = false;
        Player player = playerRepo.findById(playerId).orElse(null);
        UserPet pet = userPetRepo.findById(session.getUserPetId()).orElse(null);
        if (pet != null) {
            long baseExp = calcExpReward(monster, pet.getLevel());
            // Apply double-exp multiplier (dblexpflag 1.5x-3x)
            double expMult = levelUpService.getExpMultiplier(player);
            // Auto-fight EXP bonus (gold=1.2x, yb=1.5x)
            if (player != null && player.getAutoFitFlag() != null && player.getAutoFitFlag() == 1) {
                if (player.getMaxAutoFitSum() != null && player.getMaxAutoFitSum() > 0) {
                    expMult *= 1.5; // YB auto-fight
                } else if (player.getSysAutoSum() != null && player.getSysAutoSum() > 0) {
                    expMult *= 1.2; // Gold auto-fight
                }
            }
            // Difficulty multiplier: 普通=1x, 困难=1.3x, 冒险=1.6x
            double diffMult = session.getDifficulty() == 3 ? 1.6 : session.getDifficulty() == 2 ? 1.3 : 1.0;
            expMult *= diffMult;
            long finalExp = (long)(baseExp * expMult);
            // Use proper level-up logic (recursive multi-level)
            leveledUp = levelUpService.addExp(pet, finalExp);
            if (leveledUp) {
                log.info("Pet {} leveled up to {}", pet.getId(), pet.getLevel());
            }
        }
        if (player != null) {
            long money = monster.getMoney() != null ? monster.getMoney() : 0;
            // Difficulty money multiplier: 普通=1x, 困难=1.2x, 冒险=1.4x
            double moneyMult = session.getDifficulty() == 3 ? 1.4 : session.getDifficulty() == 2 ? 1.2 : 1.0;
            money = (long)(money * moneyMult);
            if (money > 0) {
                int current = player.getMoney() != null ? player.getMoney() : 0;
                player.setMoney(current + (int) money);
            }
            // Consume auto-fight count + recover HP/MP (PHP Fight_Mod.php lines 662-732)
            if (player.getAutoFitFlag() != null && player.getAutoFitFlag() == 1) {
                boolean ybMode = player.getMaxAutoFitSum() != null && player.getMaxAutoFitSum() > 0;
                boolean goldMode = player.getSysAutoSum() != null && player.getSysAutoSum() > 0;
                // HP/MP recovery for next battle
                if (pet != null) {
                    long maxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
                    long maxMp = (pet.getSrcmp() != null ? pet.getSrcmp() : 50) + (pet.getAddmp() != null ? pet.getAddmp() : 0);
                    pet.setHp(maxHp);
                    if (ybMode) pet.setMp(maxMp);
                    else if (goldMode) pet.setMp(maxMp / 2);
                    userPetRepo.save(pet);
                }
                if (ybMode) {
                    player.setMaxAutoFitSum(player.getMaxAutoFitSum() - 1);
                } else if (goldMode) {
                    player.setSysAutoSum(player.getSysAutoSum() - 1);
                }
                // Auto-stop if both counts exhausted
                if ((player.getMaxAutoFitSum() == null || player.getMaxAutoFitSum() <= 0) &&
                    (player.getSysAutoSum() == null || player.getSysAutoSum() <= 0)) {
                    player.setAutoFitFlag(0);
                }
            }
            playerRepo.save(player);
        }
        // Add dropped items to player bag
        String droplist = monster.getDroplist();
        if (droplist != null && !droplist.isEmpty()) {
            addDropsToBag(playerId, droplist);
        }
        return leveledUp;
    }

    private void addDropsToBag(Integer playerId, String droplist) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return;
        int maxBag = player.getMaxBag() != null ? player.getMaxBag() : 30;

        List<UserBag> currentBag = bagRepo.findByPlayerId(playerId.longValue());
        long currentTypes = currentBag.stream()
            .filter(b -> b.getVary() == null || b.getVary() == 1)
            .map(UserBag::getPropId).filter(Objects::nonNull).distinct().count();

        // Roll dice for each entry independently (PHP getProps logic)
        for (String part : droplist.split(",")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            try {
                long propId = Long.parseLong(kv[0].trim());
                int probability = Integer.parseInt(kv[1].trim());
                // 1-in-N chance per item
                if (rng.nextInt(probability) >= 1) continue;

                List<UserBag> existing = currentBag.stream()
                    .filter(b -> b.getPropId() != null && b.getPropId() == propId && (b.getVary() == null || b.getVary() == 1))
                    .collect(java.util.stream.Collectors.toList());
                if (!existing.isEmpty()) {
                    UserBag bag = existing.get(0);
                    bag.setSums((bag.getSums() != null ? bag.getSums() : 0) + 1);
                    bagRepo.save(bag);
                } else {
                    if (currentTypes >= maxBag) continue;
                    UserBag bag = new UserBag();
                    bag.setPlayerId(playerId.longValue());
                    bag.setPropId(propId);
                    bag.setSums(1);
                    bag.setVary(1);
                    bag.setStime(System.currentTimeMillis() / 1000);
                    bagRepo.save(bag);
                    currentTypes++;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid drop entry: {}", part);
            }
        }
    }

    /**
     * Auto-advance dungeon progress when a monster is killed.
     * Matches PHP fbfightGate.php: updates fuben gwid immediately on kill.
     * The nextWave fallback will handle cases where this method didn't fire.
     */
    private void advanceDungeonProgress(BattleSession session) {
        Integer mapId = session.getMapId();
        if (mapId == null) {
            log.warn("advanceDungeonProgress: mapId is null, cannot advance progress");
            return;
        }
        var diOpt = DungeonConfig.getById(mapId);
        if (diOpt.isEmpty()) return;
        var di = diOpt.get();

        Long uid = session.getPlayerId();
        var fuben = fubenRepo.findByPlayerIdAndInmap(uid, mapId).orElse(null);
        if (fuben == null) {
            log.warn("advanceDungeonProgress: no fuben record for uid={} mapId={}, creating", uid, mapId);
            fuben = new Fuben();
            fuben.setPlayerId(uid);
            fuben.setInmap(mapId);
            fuben.setGwId(0);
        }

        long now = System.currentTimeMillis() / 1000;
        int totalWaves = di.monsterIds().size();
        int currentGwId = fuben.getGwId() != null ? fuben.getGwId() : 0;
        if (currentGwId > totalWaves) currentGwId = 0; // PHP legacy guard

        int nextIdx = currentGwId + 1;
        fuben.setSrctime((long) di.cooldown());
        fuben.setLttime(now);
        fuben.setGwId(nextIdx);
        fubenRepo.save(fuben);
        log.info("advanceDungeonProgress: gwid {} -> {} uid={} mapId={}", currentGwId, nextIdx, uid, mapId);
    }
}
