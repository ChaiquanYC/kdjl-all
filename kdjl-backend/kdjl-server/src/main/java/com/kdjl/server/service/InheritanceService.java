package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pet inheritance/transfer system matching PHP Chuancheng.php.
 * State machine: muchang 1→3(joined)→4(paired)→5(waiting)→6(breeding 24h)→7(done)
 * Formula: new = (base + own*Lv/400 + mate*Lv/800) * pearl
 */
@Service
public class InheritanceService {

    private final UserPetRepository userPetRepo;
    private final PetRepository petTemplateRepo;
    private final PlayerRepository playerRepo;
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;

    private static final Set<Integer> FORBIDDEN_PETS = Set.of(103, 104, 105);
    private static final long BREED_TIME = 86400; // 24 hours

    public InheritanceService(UserPetRepository userPetRepo, PetRepository petTemplateRepo,
                              PlayerRepository playerRepo, UserBagRepository bagRepo,
                              PropsRepository propsRepo) {
        this.userPetRepo = userPetRepo;
        this.petTemplateRepo = petTemplateRepo;
        this.playerRepo = playerRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
    }

    /** List other players' pets available for inheritance (muchang=3, wx=6) */
    public List<Map<String, Object>> listAvailable(Long playerId) {
        return userPetRepo.findAll().stream()
            .filter(p -> p.getMuchang() != null && p.getMuchang() == 3
                && !p.getPlayerId().equals(playerId)
                && p.getWx() != null && p.getWx() == 6)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("level", p.getLevel());
                m.put("czl", p.getCzl());
                m.put("ownerId", p.getPlayerId());
                return m;
            }).collect(Collectors.toList());
    }

    /** Join inheritance: put pet into pairing pool (muchang=3) */
    @Transactional
    public Map<String, Object> joinInherit(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");
        if (FORBIDDEN_PETS.contains(pet.getId().intValue()))
            throw new IllegalArgumentException("该宠物禁止传承");

        // Validate conditions
        if (pet.getWx() == null || pet.getWx() != 6)
            throw new IllegalArgumentException("只有神宠(wx=6)才能传承");
        if (pet.getLevel() == null || pet.getLevel() < 90)
            throw new IllegalArgumentException("宠物等级不足90级");
        if (pet.getCzl() == null || Double.parseDouble(pet.getCzl()) < 60)
            throw new IllegalArgumentException("宠物成长率不足60");

        // Check not in another state
        int mc = pet.getMuchang() != null ? pet.getMuchang() : 1;
        if (mc != 1) throw new IllegalArgumentException("宠物当前状态不能加入传承");

        // Check 24h cooldown
        if (pet.getChchengtime() != null) {
            long elapsed = System.currentTimeMillis() / 1000 - pet.getChchengtime();
            if (elapsed < BREED_TIME)
                throw new IllegalArgumentException("每24小时只能传承一次");
        }

        // Check no equipment
        if (pet.getZb() != null && !pet.getZb().isEmpty()) {
            for (String pair : pet.getZb().split(",")) {
                String[] kv = pair.split(":");
                if (kv.length >= 2) {
                    Long bagId = Long.parseLong(kv[1]);
                    UserBag item = bagRepo.findById(bagId).orElse(null);
                    if (item != null && item.getPropId() != null) {
                        Props props = propsRepo.findById(item.getPropId().longValue()).orElse(null);
                        if (props != null && props.getVaryname() != null && props.getVaryname() == 9)
                            throw new IllegalArgumentException("请先卸下所有装备");
                    }
                }
            }
        }

        pet.setMuchang(3);
        pet.setChchengbb(0L);
        userPetRepo.save(pet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("joined", true);
        result.put("petId", petId);
        result.put("petName", pet.getName());
        result.put("state", "joined");
        return result;
    }

    /** Cancel: retrieve pet from any state except breeding (muchang=6) */
    @Transactional
    public Map<String, Object> cancelInherit(Long playerId, Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");

        int mc = pet.getMuchang() != null ? pet.getMuchang() : 1;
        if (mc == 6) throw new IllegalArgumentException("传承进行中，无法取消");
        if (mc == 1) throw new IllegalArgumentException("宠物不在传承状态");

        // If paired (muchang=4), also release mate's chchengbb
        if (mc == 4 && pet.getChchengbb() != null && pet.getChchengbb() > 0) {
            UserPet mate = userPetRepo.findById(pet.getChchengbb()).orElse(null);
            if (mate != null && mate.getChchengbb() != null && mate.getChchengbb().equals(petId)) {
                mate.setChchengbb(0L);
                mate.setMuchang(3);
                userPetRepo.save(mate);
            }
        }

        pet.setMuchang(1);
        pet.setChchengbb(0L);
        pet.setChchengwp("");
        userPetRepo.save(pet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cancelled", true);
        result.put("petId", petId);
        return result;
    }

    /** Pair with another player's pet (muchang=4 for both) */
    @Transactional
    public Map<String, Object> pairPets(Long playerId, Long myPetId, Long otherPetId) {
        UserPet myPet = userPetRepo.findById(myPetId)
            .orElseThrow(() -> new IllegalArgumentException("你的宠物不存在"));
        if (!myPet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");

        UserPet otherPet = userPetRepo.findById(otherPetId)
            .orElseThrow(() -> new IllegalArgumentException("对方宠物不存在"));
        if (otherPet.getMuchang() == null || otherPet.getMuchang() != 3)
            throw new IllegalArgumentException("对方宠物不在配对池");
        if (otherPet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不能和自己的宠物配对");

        // Both must be in state 3
        if (myPet.getMuchang() == null || myPet.getMuchang() != 3)
            throw new IllegalArgumentException("你的宠物不在配对池，请先加入");

        myPet.setMuchang(4);
        myPet.setChchengbb(otherPetId);
        userPetRepo.save(myPet);

        otherPet.setMuchang(4);
        otherPet.setChchengbb(myPetId);
        userPetRepo.save(otherPet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paired", true);
        result.put("myPet", myPet.getName());
        result.put("matePet", otherPet.getName());
        return result;
    }

    /** Confirm and start breeding (muchang=5→6) */
    @Transactional
    public Map<String, Object> startBreeding(Long playerId, Long petId, Long pearlBagId, Long skillBagId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");

        int mc = pet.getMuchang() != null ? pet.getMuchang() : 1;
        if (mc != 4 && mc != 5) throw new IllegalArgumentException("宠物未配对");

        // Consume inheritance pearl and skill preservation items
        consumeItem(playerId, pearlBagId);
        if (skillBagId != null && skillBagId > 0) consumeItem(playerId, skillBagId);

        // Check mate's state
        UserPet mate = null;
        if (pet.getChchengbb() != null && pet.getChchengbb() > 0)
            mate = userPetRepo.findById(pet.getChchengbb()).orElse(null);

        if (mate != null && mate.getMuchang() != null && mate.getMuchang() == 5) {
            // Both confirmed → start breeding
            long now = System.currentTimeMillis() / 1000;
            pet.setMuchang(6);
            pet.setChchengtime(now);
            pet.setChchengwp((pearlBagId != null ? pearlBagId : "") + "," + (skillBagId != null ? skillBagId : ""));
            userPetRepo.save(pet);

            mate.setMuchang(6);
            mate.setChchengtime(now);
            userPetRepo.save(mate);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("started", true);
            result.put("completeAt", now + BREED_TIME);
            result.put("message", "传承开始！24小时后可取回");
            return result;
        } else if (mate != null) {
            // Waiting for mate
            pet.setMuchang(5);
            userPetRepo.save(pet);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("waiting", true);
            result.put("message", "等待对方确认传承");
            return result;
        }

        throw new IllegalArgumentException("对方宠物异常");
    }

    /** Complete inheritance: apply formula, produce new pet (muchang 6→7) */
    @Transactional
    public Map<String, Object> completeInherit(Long playerId, Long petId, boolean useCrystals) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");
        if (pet.getMuchang() == null || pet.getMuchang() != 6)
            throw new IllegalArgumentException("宠物未在传承中");

        // Check time or pay crystals
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - (pet.getChchengtime() != null ? pet.getChchengtime() : now);
        long remaining = Math.max(0, BREED_TIME - elapsed);

        if (remaining > 0 && !useCrystals) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("notReady", true);
            r.put("remainingSeconds", remaining);
            r.put("crystalCost", remaining / 60);
            r.put("message", "还需等待 " + (remaining / 60) + " 分钟，或消耗 " + (remaining / 60) + " 水晶立即完成");
            return r;
        }

        if (remaining > 0 && useCrystals) {
            int cost = (int) (remaining / 60);
            Player player = playerRepo.findById(playerId.intValue()).orElse(null);
            if (player == null) throw new IllegalArgumentException("玩家数据异常");
            if (player.getYb() == null || player.getYb() < cost)
                throw new IllegalArgumentException("水晶不足，需要" + cost + "水晶");
            player.setYb(player.getYb() - cost);
            playerRepo.save(player);
        }

        // Get mate's stored stats (chchengsx field: ac,mc,srchp,hits,miss,speed,srcmp,level)
        UserPet mate = pet.getChchengbb() != null ? userPetRepo.findById(pet.getChchengbb()).orElse(null) : null;
        long[] mateStats = parseChchengsx(mate != null ? mate.getChchengsx() : null);
        long[] ownStats = parseChchengsx(pet.getChchengsx());

        // Get base pet template stats
        Pet template = petTemplateRepo.findByName(pet.getName()).orElse(null);

        // Read pearl multiplier from chchengwp
        double pearl = 1.5; // default
        String wp = pet.getChchengwp();
        if (wp != null && !wp.isEmpty()) {
            String[] wpParts = wp.split(",");
            if (wpParts.length > 0 && !wpParts[0].isEmpty()) {
                Long propId = Long.parseLong(wpParts[0]);
                Props prop = propsRepo.findById(propId).orElse(null);
                if (prop != null && prop.getEffect() != null) {
                    String[] ef = prop.getEffect().split(":");
                    if (ef.length >= 2 && "chuanc".equals(ef[0])) {
                        pearl = Double.parseDouble(ef[1]);
                    }
                }
            }
        }

        long baseAc = template != null && template.getAc() != null ? template.getAc() : 10;
        long baseMc = template != null && template.getMc() != null ? template.getMc() : 10;
        long baseHp = template != null && template.getHp() != null ? template.getHp() : 100;
        long baseMp = template != null && template.getMp() != null ? template.getMp() : 50;
        long baseHits = template != null && template.getHits() != null ? template.getHits() : 100;
        long baseMiss = template != null && template.getMiss() != null ? template.getMiss() : 0;
        long baseSpeed = template != null && template.getSpeed() != null ? template.getSpeed() : 10;

        int ownLv = pet.getLevel() != null ? pet.getLevel() : 90;
        int mateLv = mateStats.length >= 8 ? (int) mateStats[7] : 90;

        // Apply formula: new = intval((base + own*Lv/400 + mate*Lv/800) * pearl)
        long newAc    = (long)((baseAc    + (ownStats.length>0?ownStats[0]:baseAc)*ownLv/400       + mateStats[0]*mateLv/800) * pearl);
        long newMc    = (long)((baseMc    + (ownStats.length>1?ownStats[1]:baseMc)*ownLv/400       + mateStats[1]*mateLv/800) * pearl);
        long newHp    = (long)((baseHp    + (ownStats.length>2?ownStats[2]:baseHp)*ownLv/400       + mateStats[2]*mateLv/800) * pearl);
        long newHits  = (long)((baseHits  + (ownStats.length>3?ownStats[3]:baseHits)*ownLv/400     + mateStats[3]*mateLv/800) * pearl);
        long newMiss  = (long)((baseMiss  + (ownStats.length>4?ownStats[4]:baseMiss)*ownLv/400     + mateStats[4]*mateLv/800) * pearl);
        long newSpeed = (long)((baseSpeed + (ownStats.length>5?ownStats[5]:baseSpeed)*ownLv/400    + mateStats[5]*mateLv/800) * pearl);
        long newMp    = (long)((baseMp    + (ownStats.length>6?ownStats[6]:baseMp)*ownLv/400       + mateStats[6]*mateLv/800) * pearl);

        // Reset pet to level 1 with new stats
        pet.setLevel(1);
        pet.setNowexp(0L);
        pet.setLexp(170L);
        pet.setAc(newAc);
        pet.setMc(newMc);
        pet.setSrchp(newHp);
        pet.setHp(newHp);
        pet.setSrcmp(newMp);
        pet.setMp(newMp);
        pet.setHits(newHits);
        pet.setMiss(newMiss);
        pet.setSpeed(newSpeed);
        pet.setMuchang(7); // done
        pet.setChchengcolor("#FF66CC");
        pet.setChchengsx("");
        pet.setChchengwp("");
        userPetRepo.save(pet);

        // Also complete mate's inheritance
        if (mate != null) {
            mate.setMuchang(7);
            mate.setChchengcolor("#FF66CC");
            userPetRepo.save(mate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", true);
        result.put("petName", pet.getName());
        result.put("newStats", Map.of(
            "ac", newAc, "mc", newMc, "hp", newHp, "mp", newMp,
            "hits", newHits, "miss", newMiss, "speed", newSpeed
        ));
        result.put("level", 1);
        if (remaining > 0) result.put("crystalsSpent", remaining / 60);
        return result;
    }

    /** Get my pets in inheritance states */
    public List<Map<String, Object>> getMyInheritance(Long playerId) {
        List<UserPet> pets = userPetRepo.findByPlayerId(playerId);
        return pets.stream()
            .filter(p -> p.getMuchang() != null && p.getMuchang() >= 3 && p.getMuchang() <= 7)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("muchang", p.getMuchang());
                m.put("chchengbb", p.getChchengbb());
                m.put("level", p.getLevel());
                m.put("czl", p.getCzl());
                if (p.getMuchang() == 6 && p.getChchengtime() != null) {
                    long remaining = BREED_TIME - (System.currentTimeMillis()/1000 - p.getChchengtime());
                    m.put("remainingSeconds", Math.max(0, remaining));
                }
                return m;
            }).collect(Collectors.toList());
    }

    private void consumeItem(Long playerId, Long bagId) {
        if (bagId == null || bagId == 0) return;
        UserBag item = bagRepo.findById(bagId).orElse(null);
        if (item == null) return;
        int remaining = item.getSums() != null ? item.getSums() : 0;
        if (remaining <= 1) bagRepo.delete(item);
        else { item.setSums(remaining - 1); bagRepo.save(item); }
    }

    private long[] parseChchengsx(String sx) {
        if (sx == null || sx.isEmpty()) return new long[0];
        String[] parts = sx.split(",");
        long[] arr = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { arr[i] = Long.parseLong(parts[i]); }
            catch (NumberFormatException e) { arr[i] = 0; }
        }
        return arr;
    }
}
