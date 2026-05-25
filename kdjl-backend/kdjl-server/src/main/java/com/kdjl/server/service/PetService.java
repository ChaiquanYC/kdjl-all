package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.battle.*;
import com.kdjl.server.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final UserPetRepository userPetRepo;
    private final PetRepository petRepo;
    private final SkillRepository skillRepo;
    private final MonsterRepository monsterRepo;
    private final PropsRepository propsRepo;
    private final UserBagRepository bagRepo;
    private final PlayerRepository playerRepo;
    private final BattleSessionManager sessionMgr;

    public PetService(UserPetRepository userPetRepo, PetRepository petRepo,
                      SkillRepository skillRepo, MonsterRepository monsterRepo,
                      PropsRepository propsRepo, UserBagRepository bagRepo,
                      PlayerRepository playerRepo, BattleSessionManager sessionMgr) {
        this.userPetRepo = userPetRepo;
        this.petRepo = petRepo;
        this.skillRepo = skillRepo;
        this.monsterRepo = monsterRepo;
        this.propsRepo = propsRepo;
        this.bagRepo = bagRepo;
        this.playerRepo = playerRepo;
        this.sessionMgr = sessionMgr;
    }

    public List<Map<String, Object>> getPlayerPets(Long playerId) {
        List<UserPet> userPets = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() == null || p.getMuchang() == 0)
            .collect(Collectors.toList());
        return userPets.stream().map(up -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", up.getId());
            m.put("name", up.getName());
            m.put("level", up.getLevel());
            m.put("hp", up.getHp());
            m.put("mp", up.getMp());
            m.put("ac", up.getAc());
            m.put("mc", up.getMc());
            m.put("speed", up.getSpeed());
            m.put("hits", up.getHits());
            m.put("miss", up.getMiss());
            m.put("element", elementName(up.getWx()));
            m.put("nowexp", up.getNowexp());
            m.put("lexp", up.getLexp());
            m.put("skillList", up.getSkillList());
            m.put("czl", up.getCzl());
            m.put("img", up.getImgstand());
            m.put("cardImg", up.getCardimg());
            m.put("zb", up.getZb()); // equipment string
            m.put("srchp", up.getSrchp()); m.put("srcmp", up.getSrcmp());
            m.put("addhp", up.getAddhp()); m.put("addmp", up.getAddmp());
            m.put("wx", up.getWx()); // raw element number
            m.put("subyl", up.getSubyl()); m.put("subsl", up.getSubsl());
            m.put("subxl", up.getSubxl()); m.put("subdl", up.getSubdl());
            m.put("subfl", up.getSubfl()); m.put("subhl", up.getSubhl());
            m.put("subkl", up.getSubkl());
            m.put("kx", up.getKx());
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getPetDetail(Long userPetId) {
        UserPet up = userPetRepo.findById(userPetId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        List<Skill> skills = skillRepo.findByPetId(up.getId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", up.getId());
        m.put("name", up.getName());
        m.put("level", up.getLevel());
        m.put("effectImg", up.getEffectimg()); // PHP mcbbshow uses effectimg
        m.put("img", up.getImgstand());
        m.put("hp", up.getHp()); m.put("mp", up.getMp());
        m.put("ac", up.getAc()); m.put("mc", up.getMc());
        m.put("speed", up.getSpeed()); m.put("hits", up.getHits()); m.put("miss", up.getMiss());
        m.put("element", elementName(up.getWx()));
        m.put("nowexp", up.getNowexp()); m.put("lexp", up.getLexp());
        m.put("zb", up.getZb());
        var growth = new LinkedHashMap<String, Integer>();
        growth.put("金", up.getSubyl()); growth.put("木", up.getSubsl()); growth.put("水", up.getSubxl());
        growth.put("火", up.getSubdl()); growth.put("土", up.getSubfl());
        growth.put("生命", up.getSubhl()); growth.put("速度", up.getSubkl());
        m.put("growth", growth);
        m.put("skills", skills.stream().map(s -> {
            var sm = new LinkedHashMap<String, Object>();
            sm.put("id", s.getId()); sm.put("name", s.getName());
            sm.put("level", s.getLevel()); sm.put("element", elementName(s.getWx()));
            sm.put("value", s.getValue());
            return sm;
        }).collect(Collectors.toList()));
        return m;
    }

    @Transactional
    public Map<String, Object> healPet(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");
        Player player = playerRepo.findById(playerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        int cost = 50; // heal cost: 50 gold
        int money = player.getMoney() != null ? player.getMoney() : 0;
        if (money < cost) throw new IllegalArgumentException("金币不足，需要" + cost + "金币");

        long maxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
        long maxMp = (pet.getSrcmp() != null ? pet.getSrcmp() : 100) + (pet.getAddmp() != null ? pet.getAddmp() : 0);
        pet.setHp(maxHp);
        pet.setMp(maxMp);
        userPetRepo.save(pet);
        player.setMoney(money - cost);
        playerRepo.save(player);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("healed", pet.getName()); r.put("hp", maxHp); r.put("mp", maxMp); r.put("cost", cost);
        return r;
    }

    @Transactional
    public Map<String, Object> healAllPets(Long playerId) {
        Player player = playerRepo.findById(playerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        List<UserPet> pets = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() == null || p.getMuchang() == 1).collect(Collectors.toList());
        int totalCost = pets.size() * 50;
        int money = player.getMoney() != null ? player.getMoney() : 0;
        if (money < totalCost) throw new IllegalArgumentException("金币不足，需要" + totalCost + "金币(共" + pets.size() + "只)");

        int healed = 0;
        for (UserPet pet : pets) {
            long maxHp = (pet.getSrchp() != null ? pet.getSrchp() : 100) + (pet.getAddhp() != null ? pet.getAddhp() : 0);
            long maxMp = (pet.getSrcmp() != null ? pet.getSrcmp() : 100) + (pet.getAddmp() != null ? pet.getAddmp() : 0);
            pet.setHp(maxHp); pet.setMp(maxMp);
            userPetRepo.save(pet); healed++;
        }
        player.setMoney(money - totalCost); playerRepo.save(player);
        return Map.of("healed", healed, "totalCost", totalCost);
    }

    @Transactional
    public Map<String, Object> setMainPet(Integer playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId.longValue()))
            throw new IllegalArgumentException("不是你的宠物");
        Player player = playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        player.setMbid(petId.intValue());
        player.setFightBb(petId.intValue());
        playerRepo.save(player);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mainPet", pet.getName()); r.put("petId", petId);
        return r;
    }

    @Cacheable(value = "petTemplate", key = "#petId")
    public Pet getPetTemplate(Long petId) {
        return petRepo.findById(petId).orElse(null);
    }

    /** List pets in ranch (muchang=1) */
    public List<Map<String, Object>> getRanchPets(Long playerId) {
        return userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() != null && p.getMuchang() == 1)
            .map(up -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", up.getId()); m.put("name", up.getName());
                m.put("level", up.getLevel()); m.put("hp", up.getHp());
                m.put("wx", up.getWx());
                m.put("czl", up.getCzl()); m.put("img", up.getImgstand());
                return m;
            }).collect(Collectors.toList());
    }

    /** Deposit pet into ranch (muchang=0 → 1). PHP mcGate.php op=s */
    @Transactional
    public Map<String, Object> depositPet(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");

        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        int maxMc = player != null && player.getMaxMc() != null ? player.getMaxMc() : 10;
        long ranchCount = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() != null && p.getMuchang() == 1).count();
        if (ranchCount >= maxMc) throw new IllegalArgumentException("牧场已满 (" + ranchCount + "/" + maxMc + ")");

        // Check at least 2 carried pets (keep at least 1)
        long carriedCount = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() == null || p.getMuchang() == 0).count();
        if (carriedCount <= 1 && (pet.getMuchang() == null || pet.getMuchang() == 0))
            throw new IllegalArgumentException("至少需要保留一只宠物");

        // Check not main pet
        if (player != null && player.getMbid() != null && player.getMbid().equals(petId.intValue()))
            throw new IllegalArgumentException("主战宠物不能寄养");

        pet.setMuchang(1);
        userPetRepo.save(pet);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("deposited", pet.getName()); r.put("petId", petId);
        return r;
    }

    /** Discard pet permanently. PHP mcGate.php op=d — costs 10000 gold */
    @Transactional
    public Map<String, Object> discardPet(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null) throw new IllegalArgumentException("玩家不存在");

        // PHP: cannot discard main battle pet
        if (player.getMbid() != null && player.getMbid().equals(petId.intValue()))
            throw new IllegalArgumentException("主战宠物不能丢弃！请先设置其他宠物为主战");

        // Delete equipped items (PHP: DELETE FROM userbag WHERE zbpets=petId)
        List<UserBag> equipped = bagRepo.findByEquipPetId(petId);
        if (!equipped.isEmpty()) bagRepo.deleteAll(equipped);

        // Delete skills (PHP: DELETE FROM skill WHERE bid=petId)
        List<com.kdjl.common.entity.Skill> skills = skillRepo.findByPetId(petId);
        if (!skills.isEmpty()) skillRepo.deleteAll(skills);

        // Delete pet (PHP: DELETE FROM userbb WHERE id=petId)
        userPetRepo.delete(pet);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("discarded", pet.getName());
        r.put("message", "操作成功!");
        return r;
    }

    /** Withdraw pet from ranch (muchang=1 → 0). PHP mcGate.php op=g */
    @Transactional
    public Map<String, Object> withdrawPet(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");
        if (pet.getMuchang() == null || pet.getMuchang() != 1)
            throw new IllegalArgumentException("该宠物不在牧场中");

        // Check carried limit (max 3)
        long carriedCount = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() == null || p.getMuchang() == 0).count();
        if (carriedCount >= 3) throw new IllegalArgumentException("携带宠物已满 (最多3只)");

        pet.setMuchang(0);
        userPetRepo.save(pet);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("withdrawn", pet.getName()); r.put("petId", petId);
        return r;
    }

    private static String elementName(Integer wx) {
        return switch (wx != null ? wx : 1) {
            case 1 -> "金"; case 2 -> "木"; case 3 -> "水";
            case 4 -> "火"; case 5 -> "土"; default -> "无";
        };
    }

    /**
     * Attempt to capture a monster. Uses PHP formula:
     *   rate = (catchv/100) * (1 - monsterHp/monsterMaxHp) + itemBonus
     *   roll = rand(1, intval(100/(rate*100)))
     *   if roll == 1: captured!
     * Supports in-battle capture (reads monster HP from BattleSession).
     */
    @Transactional
    public Map<String, Object> capture(Long playerId, Long monsterId, BattleSession battleSession) {
        Monster monster = monsterRepo.findById(monsterId)
            .orElseThrow(() -> new IllegalArgumentException("怪物不存在"));

        // Require capture item (catchid)
        Long catchItemId = monster.getCatchItemId();
        if (catchItemId == null || catchItemId == 0) {
            throw new IllegalArgumentException("该怪物无法捕捉");
        }

        UserBag captureItem = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().longValue() == catchItemId.longValue())
            .findFirst()
            .orElse(null);
        if (captureItem == null || (captureItem.getSums() != null && captureItem.getSums() <= 0)) {
            Props needed = propsRepo.findById(catchItemId).orElse(null);
            String itemName = needed != null ? needed.getName() : "捕捉道具";
            throw new IllegalArgumentException("背包中没有" + itemName + "，无法捕捉");
        }

        // Check pet capacity (max 3 carrying, not counting in ranch)
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        int maxMc = player != null && player.getMaxMc() != null ? player.getMaxMc() : 3;
        long currentPetCount = userPetRepo.findByPlayerId(playerId).stream()
            .filter(p -> p.getMuchang() == null || p.getMuchang() == 1).count();
        if (currentPetCount >= maxMc) {
            throw new IllegalArgumentException("宠物栏已满（最多" + maxMc + "只），无法捕捉");
        }

        // Consume one capture item
        int remaining = captureItem.getSums() != null ? captureItem.getSums() : 0;
        if (remaining <= 1) {
            bagRepo.delete(captureItem);
        } else {
            captureItem.setSums(remaining - 1);
            bagRepo.save(captureItem);
        }

        // PHP capture formula: rate = (catchv/100) * (1 - hp/maxHp) + itemBonus
        int catchv = monster.getCatchv() != null ? monster.getCatchv() : 20;
        long monsterMaxHp = monster.getHp() != null ? monster.getHp() : 100;
        long monsterCurrentHp = monsterMaxHp;

        // In-battle: use live HP from BattleSession
        if (battleSession != null) {
            monsterCurrentHp = battleSession.getMonsterHp();
            monsterMaxHp = battleSession.getMonsterMaxHp();
        }

        // Read item bonus from props.effect (e.g. "catch:1|2|3:5%:2" → 5%)
        double itemBonus = 0;
        Props props = propsRepo.findById(captureItem.getPropId().longValue()).orElse(null);
        if (props != null && props.getEffect() != null) {
            String[] pv = props.getEffect().split(":");
            if (pv.length >= 3) {
                try { itemBonus = Double.parseDouble(pv[2].replace("%", "")) / 100.0; }
                catch (NumberFormatException ignored) {}
            }
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double hpRatio = (double) monsterCurrentHp / Math.max(1, monsterMaxHp);
        double rate = (catchv / 100.0) * (1.0 - hpRatio) + itemBonus;
        rate = Math.max(0.01, Math.min(0.99, rate)); // clamp 1%-99%

        double randNum = rate * 100;
        int a = randNum <= 0 ? 10000 : (int) (100.0 / randNum);
        int nvl = rng.nextInt(1, Math.max(2, a + 1));
        boolean captured = nvl == 1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monsterId", monsterId);
        result.put("monsterName", monster.getName());
        result.put("captured", captured);
        result.put("rate", String.format("%.1f%%", rate * 100));
        result.put("monsterHp", monsterCurrentHp + "/" + monsterMaxHp);
        result.put("itemConsumed", true);

        if (captured) {
            UserPet newPet = createPetFromCapture(playerId, monster);
            result.put("newPetId", newPet.getId());
            result.put("petName", newPet.getName());
            result.put("petLevel", newPet.getLevel());

            // Broadcast capture announcement (TODO: WebSocket broadcast)
            String nickname = player != null ? player.getNickname() : "冒险者";
            result.put("announce", nickname + " 成功捕捉到了 " + monster.getName() + "！");
        }

        return result;
    }

    private UserPet createPetFromCapture(Long playerId, Monster monster) {
        UserPet pet = new UserPet();
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);

        pet.setPlayerId(playerId);
        pet.setUsername(player != null ? player.getNickname() : "");
        pet.setName(monster.getName());
        pet.setLevel(monster.getLevel() != null ? monster.getLevel() : 1);
        pet.setWx(monster.getWx() != null ? monster.getWx() : 1);
        pet.setAc(monster.getAc() != null ? monster.getAc() : 10);
        pet.setMc(monster.getMc() != null ? monster.getMc() : 10);
        pet.setSrchp(monster.getHp() != null ? monster.getHp() : 100);
        pet.setHp(monster.getHp() != null ? monster.getHp() : 100);
        pet.setSrcmp(monster.getMp() != null ? monster.getMp() : 50);
        pet.setMp(monster.getMp() != null ? monster.getMp() : 50);
        pet.setAddhp(0L);
        pet.setAddmp(0L);
        pet.setSpeed(monster.getSpeed() != null ? monster.getSpeed() : 10);
        pet.setHits(monster.getHits() != null ? monster.getHits() : 100);
        pet.setMiss(monster.getMiss() != null ? monster.getMiss() : 0);
        pet.setNowexp(0L);
        pet.setLexp(55L);
        pet.setStime(System.currentTimeMillis() / 1000);

        // Try to match Pet template by name for extra fields
        Pet template = petRepo.findByName(monster.getName()).orElse(null);
        if (template != null) {
            pet.setImgstand(template.getImgstand());
            pet.setImgack(template.getImgack());
            pet.setImgdie(template.getImgdie());
            pet.setHeadimg(template.getHeadimg());
            pet.setCardimg(template.getCardimg());
            pet.setEffectimg(template.getHeadimg());
            pet.setSkillList(template.getSkillList());
            pet.setCzl("1");
            pet.setSubyl(template.getSubyl());
            pet.setSubsl(template.getSubsl());
            pet.setSubxl(template.getSubxl());
            pet.setSubdl(template.getSubdl());
            pet.setSubfl(template.getSubfl());
            pet.setSubhl(template.getSubhl());
            pet.setSubkl(template.getSubkl());
        } else {
            pet.setImgstand("");
            pet.setImgack("");
            pet.setImgdie("");
            pet.setHeadimg("");
            pet.setCardimg("");
            pet.setEffectimg("");
            pet.setSkillList("");
            pet.setCzl("1");
            pet.setSubyl(0); pet.setSubsl(0); pet.setSubxl(0);
            pet.setSubdl(0); pet.setSubfl(0); pet.setSubhl(0); pet.setSubkl(0);
        }

        return userPetRepo.save(pet);
    }
}
