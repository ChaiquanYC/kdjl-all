package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepo;
    private final SkillSysRepository skillSysRepo;
    private final UserPetRepository userPetRepo;
    private final PlayerRepository playerRepo;
    private final PetRepository petRepo;
    private final UserBagRepository bagRepo;

    private static final int MAX_SKILLS = 6;
    private static final int MAX_SKILL_LEVEL = 10;
    private static final int UPGRADE_ITEM_NORMAL = 733;
    private static final int UPGRADE_ITEM_PASSIVE = 1666;

    public SkillService(SkillRepository skillRepo, SkillSysRepository skillSysRepo,
                        UserPetRepository userPetRepo, PlayerRepository playerRepo,
                        PetRepository petRepo, UserBagRepository bagRepo) {
        this.skillRepo = skillRepo;
        this.skillSysRepo = skillSysRepo;
        this.userPetRepo = userPetRepo;
        this.playerRepo = playerRepo;
        this.petRepo = petRepo;
        this.bagRepo = bagRepo;
    }

    public List<Map<String, Object>> getPetSkills(Long petId) {
        return skillRepo.findByPetId(petId).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("skillDefId", s.getSkillDefId());
            m.put("name", s.getName());
            m.put("level", s.getLevel());
            m.put("element", elementName(s.getWx()));
            m.put("value", s.getValue());
            m.put("plus", s.getPlus());
            m.put("uhp", s.getUhp());
            m.put("ump", s.getUmp());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLearnableSkills(Long petId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        Pet template = petRepo.findByName(pet.getName()).orElse(null);
        if (template == null) return List.of();

        List<SkillSys> skillDefs = skillSysRepo.findByPid(template.getId());
        List<Skill> currentSkills = skillRepo.findByPetId(petId);
        Set<Long> knownSids = currentSkills.stream()
            .map(Skill::getSkillDefId).filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return skillDefs.stream()
            .filter(sd -> !knownSids.contains(sd.getId()))
            .map(sd -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", sd.getId());
                m.put("name", sd.getName());
                m.put("element", elementName(sd.getWx()));
                m.put("ackvalue", sd.getAckvalue());
                m.put("requires", sd.getRequires());
                m.put("wx", sd.getWx());
                m.put("pid", sd.getPid()); // skill book prop id
                return m;
            }).collect(Collectors.toList());
    }

    /**
     * Learn a new skill. Requires skill book item in bag. Matches PHP get.Skill.php.
     */
    @Transactional
    public Map<String, Object> learnSkill(Long playerId, Long petId, Long skillSysId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("不是你的宠物");
        }

        List<Skill> currentSkills = skillRepo.findByPetId(petId);
        if (currentSkills.size() >= MAX_SKILLS) {
            throw new IllegalArgumentException("宠物技能已满（最多" + MAX_SKILLS + "个）");
        }

        SkillSys wp = skillSysRepo.findById(skillSysId)
            .orElseThrow(() -> new IllegalArgumentException("技能模板不存在"));

        // Check not already known
        boolean known = currentSkills.stream()
            .anyMatch(s -> s.getSkillDefId() != null && s.getSkillDefId().equals(skillSysId));
        if (known) throw new IllegalArgumentException("宠物已学会该技能");

        // Check userbag has skill book (pid == skillsys.pid)
        Long bookPropId = wp.getPid();
        UserBag book = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(bookPropId) && (b.getSums() == null || b.getSums() > 0))
            .findFirst().orElse(null);
        if (book == null) {
            throw new IllegalArgumentException("背包中没有技能书，需要道具ID: " + bookPropId);
        }

        // Check pet level requirement
        String requires = wp.getRequires();
        if (requires != null && !requires.isEmpty()) {
            String[] larr = requires.split(",");
            if (larr.length > 0) {
                String[] bl = larr[0].split(":");
                int requiredLevel = Integer.parseInt(bl.length > 1 ? bl[1] : bl[0]);
                int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;
                if (petLevel < requiredLevel) throw new IllegalArgumentException("宠物等级不足，需要" + requiredLevel + "级");

                // Check wx
                if (wp.getWx() != null && wp.getWx() != 0 && !wp.getWx().equals(pet.getWx())) {
                    throw new IllegalArgumentException("宠物五行不匹配");
                }

                // Check unique skill (only for specific pet)
                if (larr.length > 2 && larr[2] != null && !larr[2].isEmpty()) {
                    String[] only = larr[2].split(":");
                    if (only.length > 1) {
                        Pet template = petRepo.findByName(pet.getName()).orElse(null);
                        if (template == null || !template.getId().toString().equals(only[1])) {
                            throw new IllegalArgumentException("该技能是其他宠物的专属技能");
                        }
                    }
                }
            }
        }

        // Parse level-1 values from arrays
        String[] ack = wp.getAckvalue() != null ? wp.getAckvalue().split(",") : new String[]{"0"};
        String[] plus = wp.getPlus() != null ? wp.getPlus().split(",") : new String[]{""};
        String[] uhpArr = wp.getUhp() != null ? wp.getUhp().split(",") : new String[]{"0"};
        String[] umpArr = wp.getUmp() != null ? wp.getUmp().split(",") : new String[]{"0"};

        // Apply permanent stat boost from imgeft (PHP line 110-148)
        if (wp.getImg() != null && !wp.getImg().isEmpty() && !"0".equals(wp.getImg())) {
            applyPermBoost(pet, wp.getImg());
        }

        // Create skill (level 1)
        Skill skill = new Skill();
        skill.setPetId(petId);
        skill.setSkillDefId(skillSysId);
        skill.setName(wp.getName());
        skill.setLevel(1);
        skill.setWx(wp.getWx());
        skill.setValue(ack[0]);
        skill.setPlus(plus.length > 0 ? plus[0] : "");
        skill.setImg("0"); // already applied as perm boost
        skill.setVary(wp.getVary());
        skill.setUhp(parseIntSafe(uhpArr.length > 0 ? uhpArr[0] : "0"));
        skill.setUmp(parseIntSafe(umpArr.length > 0 ? umpArr[0] : "0"));
        skill = skillRepo.save(skill);

        // Update pet skillist
        String currentSkillList = pet.getSkillList();
        String newSkillList = (currentSkillList == null || currentSkillList.isEmpty())
            ? skillSysId + ":1"
            : currentSkillList + "," + skillSysId + ":1";
        pet.setSkillList(newSkillList);
        userPetRepo.save(pet);

        // Consume skill book
        int remaining = book.getSums() != null ? book.getSums() : 0;
        if (remaining <= 1) bagRepo.delete(book);
        else { book.setSums(remaining - 1); bagRepo.save(book); }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("learned", wp.getName());
        result.put("skillId", skill.getId());
        result.put("itemConsumed", bookPropId);
        return result;
    }

    /**
     * Upgrade a skill. Requires upgrade item (733/1666). Matches PHP get.sjSkill.php.
     */
    @Transactional
    public Map<String, Object> upgradeSkill(Long playerId, Long petId, Long skillId) {
        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("不是你的宠物");
        }

        Skill skill = skillRepo.findById(skillId)
            .orElseThrow(() -> new IllegalArgumentException("技能不存在"));
        if (!skill.getPetId().equals(petId)) {
            throw new IllegalArgumentException("该技能不属于此宠物");
        }

        Long skillDefId = skill.getSkillDefId();
        if (skillDefId == null) throw new IllegalArgumentException("技能数据异常");

        SkillSys wp = skillSysRepo.findById(skillDefId)
            .orElseThrow(() -> new IllegalArgumentException("技能模板不存在"));

        int currentLevel = skill.getLevel() != null ? skill.getLevel() : 1;
        if (currentLevel >= MAX_SKILL_LEVEL) throw new IllegalArgumentException("技能已达最高等级");

        // Check upgrade item in bag
        int upgradeItemId = (wp.getVary() != null && wp.getVary().equals("4")) ? UPGRADE_ITEM_PASSIVE : UPGRADE_ITEM_NORMAL;
        UserBag upgradeItem = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId() == upgradeItemId && (b.getSums() == null || b.getSums() > 0))
            .findFirst().orElse(null);
        if (upgradeItem == null) {
            throw new IllegalArgumentException("背包中没有技能升级道具，需要道具ID: " + upgradeItemId);
        }

        // Check pet level requirement
        String requires = wp.getRequires();
        if (requires != null && !requires.isEmpty()) {
            String[] larr = requires.split(",");
            if (larr.length > currentLevel) {
                int requiredLevel = Integer.parseInt(larr[currentLevel].replaceAll("[^0-9]", "0"));
                int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;
                if (petLevel < requiredLevel) throw new IllegalArgumentException("宠物等级不足，需要" + requiredLevel + "级");
            }
        }

        // Parse arrays
        String[] ack = wp.getAckvalue() != null ? wp.getAckvalue().split(",") : new String[]{"0"};
        String[] plus = wp.getPlus() != null ? wp.getPlus().split(",") : new String[]{""};
        String[] uhpArr = wp.getUhp() != null ? wp.getUhp().split(",") : new String[]{"0"};
        String[] umpArr = wp.getUmp() != null ? wp.getUmp().split(",") : new String[]{"0"};

        int newLevel = currentLevel + 1;
        int idx = currentLevel; // array index for new level's values

        // Apply permanent stat boost from imgeft for new level
        if (wp.getImg() != null && !wp.getImg().isEmpty() && !"0".equals(wp.getImg())) {
            // The imgeft field might be comma-separated per level
            String[] imgArr = wp.getImg() != null ? wp.getImg().split(",") : new String[]{};
            if (imgArr.length > idx && !"0".equals(imgArr[idx])) {
                applyPermBoost(pet, imgArr[idx]);
            }
        }

        // Update skill to next level
        skill.setLevel(newLevel);
        skill.setValue(idx < ack.length ? ack[idx] : ack[ack.length - 1]);
        skill.setPlus(idx < plus.length ? plus[idx] : (plus.length > 0 ? plus[plus.length - 1] : ""));
        skill.setUhp(idx < uhpArr.length ? parseIntSafe(uhpArr[idx]) : (uhpArr.length > 0 ? parseIntSafe(uhpArr[uhpArr.length - 1]) : 0));
        skill.setUmp(idx < umpArr.length ? parseIntSafe(umpArr[idx]) : (umpArr.length > 0 ? parseIntSafe(umpArr[umpArr.length - 1]) : 0));
        skillRepo.save(skill);

        // Consume upgrade item
        int remaining = upgradeItem.getSums() != null ? upgradeItem.getSums() : 0;
        if (remaining <= 1) bagRepo.delete(upgradeItem);
        else { upgradeItem.setSums(remaining - 1); bagRepo.save(upgradeItem); }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("upgraded", skill.getName());
        result.put("newLevel", newLevel);
        result.put("itemConsumed", upgradeItemId);
        return result;
    }

    /**
     * Apply permanent stat boost from skill image effect. Matches PHP get.Skill.php lines 110-148.
     * Format: "addac:5%" or "addmc:10" or "addhp:3%"
     */
    private void applyPermBoost(UserPet pet, String img) {
        if (img == null || img.isEmpty() || "0".equals(img)) return;
        String[] imgarr = img.split(":");
        if (imgarr.length < 2) return;
        String type = imgarr[0];
        String valStr = imgarr[1].replace("%", "");
        double num;
        try { num = Double.parseDouble(valStr) / 100.0; }
        catch (NumberFormatException e) { return; }

        boolean isPercent = imgarr[1].contains("%");
        switch (type) {
            case "addac" -> {
                long cur = pet.getAc() != null ? pet.getAc() : 0;
                pet.setAc(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
            case "addmc" -> {
                long cur = pet.getMc() != null ? pet.getMc() : 0;
                pet.setMc(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
            case "addhp" -> {
                long cur = pet.getSrchp() != null ? pet.getSrchp() : 0;
                pet.setSrchp(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
            case "addmp" -> {
                long cur = pet.getSrcmp() != null ? pet.getSrcmp() : 0;
                pet.setSrcmp(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
            case "addhits" -> {
                long cur = pet.getHits() != null ? pet.getHits() : 0;
                pet.setHits(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
            case "addspeed" -> {
                long cur = pet.getSpeed() != null ? pet.getSpeed() : 0;
                pet.setSpeed(isPercent ? Math.round(cur * (1 + num)) : cur + (long)(num * 100));
            }
        }
        userPetRepo.save(pet);
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9\\-]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String elementName(Integer wx) {
        return switch (wx != null ? wx : 1) {
            case 1 -> "金"; case 2 -> "木"; case 3 -> "水";
            case 4 -> "火"; case 5 -> "土"; default -> "无";
        };
    }
}
