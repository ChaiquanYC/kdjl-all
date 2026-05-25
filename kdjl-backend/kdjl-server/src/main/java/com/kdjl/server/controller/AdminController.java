package com.kdjl.server.controller;

import com.kdjl.common.dto.ApiResponse;
import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserPetRepository userPetRepo;
    private final PetRepository petRepo;
    private final PlayerRepository playerRepo;
    private final UserBagRepository bagRepo;
    private final SkillSysRepository skillSysRepo;
    private final SkillRepository skillRepo;

    public AdminController(UserPetRepository userPetRepo, PetRepository petRepo,
                           PlayerRepository playerRepo, UserBagRepository bagRepo,
                           SkillSysRepository skillSysRepo, SkillRepository skillRepo) {
        this.userPetRepo = userPetRepo;
        this.petRepo = petRepo;
        this.playerRepo = playerRepo;
        this.bagRepo = bagRepo;
        this.skillSysRepo = skillSysRepo;
        this.skillRepo = skillRepo;
    }

    /** Reset all pets for a player to level 1 */
    @PostMapping("/reset-pets")
    public ApiResponse<String> resetPets(@RequestParam Integer uid) {
        List<UserPet> pets = userPetRepo.findByPlayerId(uid.longValue());
        for (UserPet p : pets) {
            Pet template = petRepo.findByName(p.getName()).orElse(null);
            if (template != null) {
                p.setLevel(1);
                p.setNowexp(0L);
                p.setLexp(100L);
                p.setSrchp(template.getHp());
                p.setSrcmp(template.getMp());
                p.setHp(template.getHp());
                p.setMp(template.getMp());
                p.setAc(template.getAc());
                p.setMc(template.getMc());
                p.setHits(template.getHits().longValue());
                p.setMiss(template.getMiss().longValue());
                p.setSpeed(template.getSpeed().longValue());
                p.setWx(template.getWx());
                p.setKx(template.getKx());
            } else {
                p.setLevel(1);
                p.setNowexp(0L);
                p.setLexp(100L);
            }
            userPetRepo.save(p);
        }
        return ApiResponse.success("Reset " + pets.size() + " pets");
    }

    /** Set player auto-fight counts directly */
    @PostMapping("/set-auto")
    public ApiResponse<Map<String, Object>> setAuto(@RequestParam int uid, @RequestParam(defaultValue = "100") int gold, @RequestParam(defaultValue = "100") int yb) {
        Player p = playerRepo.findById(uid).orElse(null);
        if (p == null) return ApiResponse.error("Player not found");
        p.setSysAutoSum(gold);
        p.setMaxAutoFitSum(yb);
        playerRepo.save(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("goldAuto", gold);
        r.put("ybAuto", yb);
        r.put("message", "Auto-fight counts set");
        return ApiResponse.success(r);
    }

    /** Add item to player bag */
    @PostMapping("/add-item")
    public ApiResponse<Map<String, Object>> addItem(@RequestParam int uid, @RequestParam long propId, @RequestParam(defaultValue = "1") int count) {
        try {
            List<UserBag> existing = bagRepo.findByPlayerId((long) uid).stream()
                .filter(b -> b.getPropId() != null && b.getPropId().equals(propId))
                .collect(java.util.stream.Collectors.toList());
            if (!existing.isEmpty()) {
                UserBag b = existing.get(0);
                b.setSums((b.getSums() != null ? b.getSums() : 0) + count);
                bagRepo.save(b);
            } else {
                UserBag b = new UserBag();
                b.setPlayerId((long) uid);
                b.setPropId(propId);
                b.setSums(count);
                b.setStime(System.currentTimeMillis() / 1000);
                b.setPyb(0);
                bagRepo.save(b);
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("added", propId);
            r.put("count", count);
            return ApiResponse.success(r);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** Directly teach a skill to a pet (no book required) */
    @PostMapping("/add-skill")
    public ApiResponse<Map<String, Object>> addSkill(@RequestParam long petId, @RequestParam long skillSysId) {
        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null) return ApiResponse.error("Pet not found");
        SkillSys wp = skillSysRepo.findById(skillSysId).orElse(null);
        if (wp == null) return ApiResponse.error("Skill template not found");

        // Check not already known
        List<Skill> existing = skillRepo.findByPetId(petId);
        boolean known = existing.stream().anyMatch(s -> s.getSkillDefId() != null && s.getSkillDefId().equals(skillSysId));
        if (known) return ApiResponse.error("Already known");

        String[] ack = wp.getAckvalue() != null ? wp.getAckvalue().split(",") : new String[]{"0"};
        String[] plus = wp.getPlus() != null ? wp.getPlus().split(",") : new String[]{""};
        String[] uhpArr = wp.getUhp() != null ? wp.getUhp().split(",") : new String[]{"0"};
        String[] umpArr = wp.getUmp() != null ? wp.getUmp().split(",") : new String[]{"0"};

        Skill skill = new Skill();
        skill.setPetId(petId);
        skill.setSkillDefId(skillSysId);
        skill.setName(wp.getName());
        skill.setLevel(1);
        skill.setWx(wp.getWx());
        skill.setValue(ack.length > 0 ? ack[0] : "0");
        skill.setPlus(plus.length > 0 ? plus[0] : "");
        skill.setImg("0");
        skill.setVary(wp.getVary());
        skill.setUhp(uhpArr.length > 0 ? Integer.parseInt(uhpArr[0].replaceAll("[^0-9\\-]", "0")) : 0);
        skill.setUmp(umpArr.length > 0 ? Integer.parseInt(umpArr[0].replaceAll("[^0-9\\-]", "0")) : 0);
        skillRepo.save(skill);

        String currentSkillList = pet.getSkillList();
        String newSkillList = (currentSkillList == null || currentSkillList.isEmpty())
            ? skillSysId + ":1"
            : currentSkillList + "," + skillSysId + ":1";
        pet.setSkillList(newSkillList);
        userPetRepo.save(pet);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("learned", wp.getName());
        r.put("petId", petId);
        return ApiResponse.success(r);
    }

    /** Add a pet from bb template to player */
    @PostMapping("/add-pet")
    public ApiResponse<Map<String, Object>> addPet(@RequestParam Integer uid, @RequestParam String petName) {
        Pet template = petRepo.findByName(petName)
            .orElseThrow(() -> new IllegalArgumentException("Pet template not found: " + petName));

        UserPet p = new UserPet();
        p.setPlayerId(uid.longValue());
        p.setName(template.getName());
        p.setLevel(1);
        p.setWx(template.getWx());
        p.setSrchp(template.getHp());
        p.setSrcmp(template.getMp());
        p.setHp(template.getHp());
        p.setMp(template.getMp());
        p.setAc(template.getAc());
        p.setMc(template.getMc());
        p.setHits(template.getHits().longValue());
        p.setMiss(template.getMiss().longValue());
        p.setSpeed(template.getSpeed().longValue());
        p.setNowexp(0L);
        p.setLexp(100L);
        p.setImgstand(template.getImgstand());
        p.setImgack(template.getImgack());
        p.setImgdie(template.getImgdie());
        p.setHeadimg(template.getHeadimg());
        p.setCardimg(template.getCardimg());
        p.setEffectimg(template.getEffectimg());
        p.setKx(template.getKx());
        p.setSkillList(template.getSkillList());
        // Generate czl from template range
        String czlRange = template.getCzl();
        if (czlRange != null && czlRange.contains(",")) {
            double czl = generateCzl(czlRange);
            p.setCzl(String.valueOf(czl));
        } else {
            p.setCzl(czlRange != null ? czlRange : "1.0");
        }
        p.setStime(System.currentTimeMillis() / 1000);
        p = userPetRepo.save(p);

        // Auto-add 普通攻击 (skill ID 1)
        SkillSys wp1 = skillSysRepo.findById(1L).orElse(null);
        if (wp1 != null) {
            Skill s = new Skill();
            s.setPetId(p.getId()); s.setSkillDefId(1L); s.setName("普通攻击");
            s.setLevel(1); s.setWx(0); s.setVary("1"); s.setValue("0"); s.setPlus("");
            s.setUhp(0); s.setUmp(0); s.setImg("0");
            skillRepo.save(s);
            p.setSkillList((p.getSkillList() != null ? p.getSkillList() + ",1:1" : "1:1"));
            userPetRepo.save(p);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", p.getId());
        result.put("name", p.getName());
        result.put("czl", p.getCzl());
        result.put("level", 1);
        return ApiResponse.success(result);
    }

    private double generateCzl(String czlRange) {
        String cleaned = czlRange.replace(".", "");
        String[] parts = cleaned.split(",");
        if (parts.length != 2) return 1.0;
        int min = Integer.parseInt(parts[0]);
        int max = Integer.parseInt(parts[1]);
        return (new java.util.Random().nextInt(min, max + 1)) / 10.0;
    }
}
