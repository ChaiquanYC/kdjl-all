package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pet shrine operations — faithful replication of PHP logic:
 * jhGate.php (evolution), cmpGate.php (compose), zsGate.php (nirvana),
 * superJhGate.php (sacred evolve), cqGate.php (extract), zhGate.php (convert),
 * sszsInfo.php (sacred rebirth).
 */
@Service
public class PetShrineService {

    private final UserPetRepository userPetRepo;
    private final PetRepository petRepo;
    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final MergeRepository mergeRepo;
    private final ZsRepository zsRepo;
    private final SuperJhRepository superJhRepo;
    private final SuperZsRepository superZsRepo;
    private final SkillRepository skillRepo;
    private final SkillSysRepository skillSysRepo;
    private final WxRepository wxRepo;
    private final CooldownService cooldownService;
    private final PetLockService petLockService;

    public PetShrineService(UserPetRepository userPetRepo, PetRepository petRepo,
                            PlayerRepository playerRepo, PlayerExtRepository playerExtRepo,
                            UserBagRepository bagRepo, PropsRepository propsRepo,
                            MergeRepository mergeRepo, ZsRepository zsRepo,
                            SuperJhRepository superJhRepo, SuperZsRepository superZsRepo,
                            SkillRepository skillRepo, SkillSysRepository skillSysRepo,
                            WxRepository wxRepo,
                            CooldownService cooldownService, PetLockService petLockService) {
        this.userPetRepo = userPetRepo;
        this.petRepo = petRepo;
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.mergeRepo = mergeRepo;
        this.zsRepo = zsRepo;
        this.superJhRepo = superJhRepo;
        this.superZsRepo = superZsRepo;
        this.skillRepo = skillRepo;
        this.skillSysRepo = skillSysRepo;
        this.wxRepo = wxRepo;
        this.cooldownService = cooldownService;
        this.petLockService = petLockService;
    }

    // ── helpers ──

    /** PHP uses czl column directly as a single float (e.g. 23.2), NOT sum of growth attrs. */
    private double czl(UserPet pet) {
        String s = pet.getCzl();
        if (s == null || s.isEmpty()) return 0;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private void setCzl(UserPet pet, double v) {
        pet.setCzl(String.valueOf(Math.round(v * 10.0) / 10.0));
    }

    private ThreadLocalRandom rng() { return ThreadLocalRandom.current(); }

    private boolean isExtracted(Long playerId, Long petId) {
        PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
        if (ext == null || ext.getChouquChongwu() == null) return false;
        return ext.getChouquChongwu().contains("," + petId + ",");
    }

    private PlayerExt getOrCreatePlayerExt(Integer playerId) {
        return playerExtRepo.findById(playerId).orElseGet(() -> {
            PlayerExt e = new PlayerExt();
            e.setPlayerId(playerId);
            return playerExtRepo.save(e);
        });
    }

    // ── item helpers ──

    /** Consume one count of a prop by pid. */
    private void consumeProp(Long playerId, int propId) {
        UserBag bag = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().intValue() == propId
                && (b.getSums() == null || b.getSums() > 0))
            .findFirst().orElse(null);
        if (bag != null) {
            int rem = bag.getSums() != null ? bag.getSums() : 0;
            if (rem <= 1) bagRepo.delete(bag);
            else { bag.setSums(rem - 1); bagRepo.save(bag); }
        }
    }

    /** Consume count of a prop by pid. */
    private void consumePropCount(Long playerId, int propId, int count) {
        UserBag bag = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().intValue() == propId
                && (b.getSums() == null || b.getSums() >= count))
            .findFirst().orElse(null);
        if (bag != null) {
            int rem = bag.getSums() != null ? bag.getSums() : 0;
            if (rem <= count) bagRepo.delete(bag);
            else { bag.setSums(rem - count); bagRepo.save(bag); }
        }
    }

    private void consumeBagItem(UserBag bag) {
        int rem = bag.getSums() != null ? bag.getSums() : 0;
        if (rem <= 1) bagRepo.delete(bag);
        else { bag.setSums(rem - 1); bagRepo.save(bag); }
    }

    private void consumeBagItemById(Long playerId, Long bagId) {
        UserBag bag = bagRepo.findById(bagId).orElse(null);
        if (bag != null && bag.getPlayerId().equals(playerId)) consumeBagItem(bag);
    }

    /** Count total owned count of a prop id. */
    private int countProp(Long playerId, int propId) {
        return bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().intValue() == propId)
            .mapToInt(b -> b.getSums() != null ? b.getSums() : 0).sum();
    }

    private Props getPropsByBagId(Long playerId, Long bagId) {
        if (bagId == null || bagId <= 0) return null;
        UserBag bag = bagRepo.findById(bagId).orElse(null);
        if (bag == null || !bag.getPlayerId().equals(playerId)) return null;
        return propsRepo.findById(bag.getPropId().longValue()).orElse(null);
    }

    // ── pet deletion (matches PHP clearBB) ──

    @Transactional
    public void deletePet(Long petId) {
        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null) return;
        skillRepo.deleteByPetId(petId);
        List<UserBag> equipped = bagRepo.findByEquipPetId(petId);
        if (!equipped.isEmpty()) bagRepo.deleteAll(equipped);
        userPetRepo.delete(pet);
    }

    // ═══════════════════════════════════════════════════
    // Tab 1: 进化 — jhGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> evolve(Long playerId, Long petId, int style, Long keepCzlItemId) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.EVOLVE);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || !pet.getPlayerId().equals(playerId)) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: if($cbb['wx']>6) die('五行属于：金、木、水、火、土、神的才可以进行此操作！')
        if (pet.getWx() != null && pet.getWx() > 6)
            return Map.of("code", 4, "message", "五行属于：金、木、水、火、土、神的才可以进行此操作！");

        // PHP: if($cbb['remaketimes'] == 10) die('6')
        int times = pet.getRemaketimes() != null ? pet.getRemaketimes() : 0;
        if (times >= 10) return Map.of("code", 4, "message", "已达最大进化次数");

        // PHP: check chouqu_chongwu
        if (isExtracted(playerId, petId))
            return Map.of("code", 4, "message", "该宠物抽取过成长,不能进行进化!");

        // PHP: $tt = split(',',$v['remakeid']); $pid = $tt[$style-1];
        // remake arrays have TWO entries [style1, style2]
        String remakeIds = pet.getRemakeid();
        String remakePids = pet.getRemakepid();
        String remakeLevels = pet.getRemakelevel();

        if (remakeIds == null || remakeIds.isEmpty() || remakeIds.equals("0,0"))
            return Map.of("code", 4, "message", "无法继续进化");

        String[] idArr = remakeIds.split(",");
        String[] pidArr = remakePids != null ? remakePids.split(",") : new String[0];
        String[] levelArr = remakeLevels != null ? remakeLevels.split(",") : new String[0];

        int idx = style - 1; // PHP: $style-1
        if (idx >= idArr.length) return Map.of("code", 4, "message", "进化链已结束");

        int targetPid = Integer.parseInt(idArr[idx].trim());
        if (targetPid == 0) return Map.of("code", 4, "message", "进化链已结束");

        String propsIdStr = idx < pidArr.length ? pidArr[idx].trim() : "0";
        int requiredLevel = idx < levelArr.length ? Integer.parseInt(levelArr[idx].trim()) : 200;
        if (pet.getLevel() < requiredLevel) return Map.of("code", 4, "message", "需要等级" + requiredLevel);

        // PHP: if ($user['money'] < 1000) die("5")
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < 1000))
            return Map.of("code", 3, "message", "金币不足，需要1000");

        // PHP: keepCzl item lookup by bhid
        double keepCzl = 0;
        if (keepCzlItemId != null && keepCzlItemId > 0) {
            UserBag bhBag = bagRepo.findById(keepCzlItemId).orElse(null);
            if (bhBag != null && bhBag.getPlayerId().equals(playerId)) {
                Props bhProps = propsRepo.findById(bhBag.getPropId().longValue()).orElse(null);
                if (bhProps != null && bhProps.getEffect() != null) {
                    String eff = bhProps.getEffect();
                    if (eff.startsWith("keepczl:")) {
                        keepCzl = Double.parseDouble(eff.substring("keepczl:".length()));
                    }
                }
            }
        }

        // PHP: required material check
        if (!propsIdStr.isEmpty() && !propsIdStr.equals("0")) {
            // propsIdStr may be pipe-separated alternatives like "74|75"
            String[] altPids = propsIdStr.split("\\|");
            boolean found = false;
            int matchedPid = 0;
            for (String alt : altPids) {
                int pid = Integer.parseInt(alt.trim());
                if (countProp(playerId, pid) > 0) { found = true; matchedPid = pid; break; }
            }
            if (!found) return Map.of("code", 2, "message", "缺少必需道具");
            // Don't consume yet — consume after success checks
        }

        // PHP: get target template by pid
        Pet sbb = petRepo.findById((long) targetPid).orElse(null);
        if (sbb == null) return Map.of("code", 10, "message", "进化目标模板不存在");

        // PHP: special items 1221/1222
        boolean has1221 = countProp(playerId, 1221) > 0;
        boolean has1222 = countProp(playerId, 1222) > 0;

        double currentCzl = czl(pet);
        double newCzl;

        int matchedPid2 = 0;
        if (!propsIdStr.isEmpty() && !propsIdStr.equals("0")) {
            for (String alt : propsIdStr.split("\\|")) {
                int pid = Integer.parseInt(alt.trim());
                if (countProp(playerId, pid) > 0) { matchedPid2 = pid; break; }
            }
        }

        // PHP: growth formula
        if (matchedPid2 == 1221) {
            // PHP: if($pids == 1221) $czl = $cbb['czl']+(rand(1,3))/10;
            newCzl = currentCzl + rng().nextInt(1, 4) / 10.0;
        } else if (matchedPid2 == 1222) {
            // PHP: if($pids == 1222) $czl = $cbb['czl']+(rand(3,6))/10;
            newCzl = currentCzl + rng().nextInt(3, 7) / 10.0;
        } else if (style == 1) {
            if (currentCzl < 50)
                newCzl = round1(currentCzl + rng().nextInt(1, 6) / 10.0 + round1((pet.getLevel() - requiredLevel) / 200.0));
            else if (currentCzl < 80)
                newCzl = currentCzl + rng().nextInt(1, 4) / 10.0;
            else
                newCzl = round1(currentCzl + 0.1);
        } else { // style == 2
            if (currentCzl < 50)
                newCzl = round1(currentCzl + rng().nextInt(5, 11) / 10.0 + round1((pet.getLevel() - requiredLevel) / 200.0));
            else if (currentCzl < 70)
                newCzl = currentCzl + rng().nextInt(4, 8) / 10.0;
            else if (currentCzl < 80)
                newCzl = currentCzl + rng().nextInt(3, 6) / 10.0;
            else if (currentCzl < 90)
                newCzl = currentCzl + rng().nextInt(2, 4) / 10.0;
            else
                newCzl = currentCzl + rng().nextInt(1, 4) / 10.0;
        }

        // PHP: cap at 150 + keepCzl protection item
        if (newCzl >= 150.0) {
            if (keepCzl >= 150) {
                if (newCzl > keepCzl) {
                    newCzl = keepCzl;
                }
                consumeBagItemById(playerId, keepCzlItemId);
            } else {
                newCzl = 150.0;
            }
        }

        // Consume items
        if (matchedPid2 != 0) consumeProp(playerId, matchedPid2);

        // PHP: update pet with target template info
        pet.setImgstand(sbb.getImgstand());
        pet.setImgack(sbb.getImgack());
        pet.setImgdie(sbb.getImgdie());
        pet.setName(sbb.getName());
        pet.setCardimg(sbb.getCardimg());
        pet.setEffectimg(sbb.getEffectimg() != null ? sbb.getEffectimg() : sbb.getHeadimg());

        // PHP: $rml=$sbb['remakelevel']; $rmid=$sbb['remakeid']; $rmpid=$sbb['remakepid'];
        // After evolution, take the remake chain from the target template
        pet.setRemakelevel(sbb.getRemakeLevel());
        pet.setRemakeid(sbb.getRemakeId());
        pet.setRemakepid(sbb.getRemakePid());

        int newTimes = times + 1;
        pet.setRemaketimes(newTimes);
        setCzl(pet, newCzl);

        userPetRepo.save(pet);

        // PHP: reduce gold
        player.setMoney(player.getMoney() - 1000);
        playerRepo.save(player);

        cooldownService.recordOp(playerId, CooldownService.Op.EVOLVE);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 1); r.put("message", "进化成功");
        r.put("czl", czl(pet)); r.put("remaketimes", newTimes);
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 2: 合成 — cmpGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> compose(Long playerId, Long mainPetId, Long subPetId, Long item1Id, Long item2Id) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.COMPOSE);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet mainPet = userPetRepo.findById(mainPetId).orElse(null);
        UserPet subPet = userPetRepo.findById(subPetId).orElse(null);
        if (mainPet == null || subPet == null) return Map.of("code", 10, "message", "宠物不存在");
        if (!mainPet.getPlayerId().equals(playerId) || !subPet.getPlayerId().equals(playerId))
            return Map.of("code", 10, "message", "不是你的宠物");
        if (mainPet.getId().equals(subPet.getId())) return Map.of("code", 10, "message", "不能使用同一只宠物");
        if (mainPet.getLevel() < 40 || subPet.getLevel() < 40) return Map.of("code", 4, "message", "需要等级40");

        if (isExtracted(playerId, mainPetId) || isExtracted(playerId, subPetId))
            return Map.of("code", 4, "message", "某个宠物抽取过成长,不能进行合成!");

        // PHP: check equipment on pets
        if ((mainPet.getZb() != null && !mainPet.getZb().isEmpty() && !mainPet.getZb().equals("0"))
            || (subPet.getZb() != null && !subPet.getZb().isEmpty() && !subPet.getZb().equals("0")))
            return Map.of("code", 1000, "message", "请先卸下宠物装备");

        // PHP: item check — both items must be varyname=8
        Props pp1 = getPropsByBagId(playerId, item1Id);
        Props pp2 = getPropsByBagId(playerId, item2Id);
        if ((item1Id != null && item1Id > 0 && (pp1 == null || pp1.getVaryname() == null || pp1.getVaryname() != 8))
            || (item2Id != null && item2Id > 0 && (pp2 == null || pp2.getVaryname() == null || pp2.getVaryname() != 8)))
            return Map.of("code", 200, "message", "请放入合成道具");

        // PHP: check same item has enough count
        if (item1Id != null && item1Id.equals(item2Id) && item1Id > 0) {
            UserBag bag = bagRepo.findById(item1Id).orElse(null);
            if (bag == null || bag.getSums() == null || bag.getSums() < 2)
                return Map.of("code", 100, "message", "道具数量不足");
        }

        // PHP: template lookup by pet NAME
        Pet mainTmpl = petRepo.findByName(mainPet.getName()).orElse(null);
        Pet subTmpl = petRepo.findByName(subPet.getName()).orElse(null);
        if (mainTmpl == null || subTmpl == null) return Map.of("code", 10, "message", "宠物模板数据异常");

        // PHP: merge formula lookup
        List<Merge> formulas = mergeRepo.findByAidAndBid(mainTmpl.getId().intValue(), subTmpl.getId().intValue());
        if (formulas.isEmpty()) return Map.of("code", 2, "message", "不能合成");

        Merge merge = formulas.get(0);

        // PHP: limits check — format "minMainCzl|minSubCzl|maxCzl"
        double maxCzlFromLimit = 0;
        if (merge.getLimits() != null && !merge.getLimits().isEmpty()) {
            String[] limitsArr = merge.getLimits().split("\\|");
            if (limitsArr.length >= 1 && !limitsArr[0].isEmpty()) {
                double minMain = Double.parseDouble(limitsArr[0]);
                if (czl(mainPet) < minMain) return Map.of("code", 15, "message", "主宠成长不足");
            }
            if (limitsArr.length >= 2 && !limitsArr[1].isEmpty()) {
                double minSub = Double.parseDouble(limitsArr[1]);
                if (czl(subPet) < minSub) return Map.of("code", 15, "message", "副宠成长不足");
            }
            if (limitsArr.length >= 3 && !limitsArr[2].isEmpty()) {
                maxCzlFromLimit = Double.parseDouble(limitsArr[2]);
            }
        }

        // PHP: gold check
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < 50000))
            return Map.of("code", 3, "message", "金币不足，需要50000");

        // PHP: parse item effects
        // Effect format: "hecheng:A:10%,B:4%|addczl:8%|1"
        // Last segment "1" means protect (shbb)
        double hechengA = 0, hechengB = 0, addczlBonus = 0;
        double addAc = 0, addMc = 0, addHit = 0, addMiss = 0, addSpeed = 0, addHp = 0, addMp = 0;
        boolean shbb1 = false, shbb2 = false;

        if (pp1 != null && pp1.getEffect() != null) {
            ComposeItemParsed p = parseComposeItem(pp1.getEffect());
            hechengA += p.hechengA; hechengB += p.hechengB;
            addczlBonus += p.addczl; addAc += p.addAc; addMc += p.addMc;
            addHit += p.addHit; addMiss += p.addMiss; addSpeed += p.addSpeed;
            addHp += p.addHp; addMp += p.addMp;
            if (p.shbb) shbb1 = true;
        }
        if (pp2 != null && pp2.getEffect() != null) {
            ComposeItemParsed p = parseComposeItem(pp2.getEffect());
            hechengA += p.hechengA; hechengB += p.hechengB;
            addczlBonus += p.addczl; addAc += p.addAc; addMc += p.addMc;
            addHit += p.addHit; addMiss += p.addMiss; addSpeed += p.addSpeed;
            addHp += p.addHp; addMp += p.addMp;
            if (p.shbb) shbb2 = true;
        }

        // PHP: getSuccess()
        PlayerExt ext = getOrCreatePlayerExt(playerId.intValue());
        int luckyStar = ext.getMergeCount() != null ? ext.getMergeCount() : 0;
        double mainCzl = czl(mainPet), subCzl = czl(subPet);

        double successRate;
        if (luckyStar >= 10 || mainCzl <= 5) {
            successRate = 1.0;
        } else {
            // PHP: $chenggonglv=($cishu['hecheng_nums']/($app['czl']*2))+(($app['level']+$bpp['level'])/15)*0.01+$arr2+(rand(1,5)*0.01)
            successRate = luckyStar / (mainCzl * 2)
                + (mainPet.getLevel() + subPet.getLevel()) / 15.0 * 0.01
                + hechengA + rng().nextInt(1, 6) * 0.01;
            successRate = round1(successRate);
        }

        boolean success = rng().nextDouble() < successRate;

        // Consume items
        if (item1Id != null && item1Id > 0) consumeBagItemById(playerId, item1Id);
        if (item2Id != null && item2Id > 0) consumeBagItemById(playerId, item2Id);

        Map<String, Object> r = new LinkedHashMap<>();

        if (success) {
            // PHP: B second roll
            int resultType;
            double bChance = 0.05 + hechengB;
            if (rng().nextDouble() < bChance) resultType = 2; // B variant
            else resultType = 1; // A variant

            int newPetId = resultType == 2 && merge.getMbid() != null ? merge.getMbid() : merge.getMaid();
            Pet template = petRepo.findById((long) newPetId).orElse(null);
            if (template == null) return Map.of("code", 10, "message", "合成模板不存在");

            // PHP: bbczl()
            double newCzl = bbczlCompose(mainCzl, mainPet.getLevel(), subCzl, subPet.getLevel(), addczlBonus);

            // PHP: wx caps
            Integer newWx = template.getWx() != null ? template.getWx() : mainPet.getWx();
            if (newWx != null && newWx == 6 && newCzl > 60) newCzl = 60;
            else if ((newWx == null || newWx != 6) && newCzl > 150) newCzl = 150;
            if (maxCzlFromLimit > 0 && newCzl > maxCzlFromLimit) newCzl = maxCzlFromLimit;

            // PHP: makebb() — create new pet
            UserPet newPet = new UserPet();
            newPet.setPlayerId(playerId);
            newPet.setUsername(player.getNickname());
            newPet.setName(template.getName());
            newPet.setLevel(1);
            newPet.setWx(newWx);
            newPet.setImgstand(template.getImgstand());
            newPet.setImgack(template.getImgack());
            newPet.setImgdie(template.getImgdie());
            newPet.setHeadimg("t" + template.getId() + ".gif");
            newPet.setCardimg("k" + template.getId() + ".gif");
            newPet.setEffectimg("q" + template.getId() + ".gif");
            newPet.setSkillList(template.getSkillList());
            newPet.setStime(System.currentTimeMillis() / 1000);
            newPet.setNowexp(0L);
            newPet.setLexp(100L);
            newPet.setKx(template.getKx());
            newPet.setRemakelevel(template.getRemakeLevel());
            newPet.setRemakeid(template.getRemakeId());
            newPet.setRemakepid(template.getRemakePid());
            newPet.setMuchang(0);
            newPet.setRemaketimes(0);
            newPet.setOldBid(template.getId().intValue());

            // PHP: getPa() for each attr
            newPet.setAc(getPa(template.getAc() != null ? template.getAc() : 10, mainPet.getAc(), mainPet.getLevel(), subPet.getAc(), subPet.getLevel(), addAc));
            newPet.setMc(getPa(template.getMc() != null ? template.getMc() : 10, mainPet.getMc(), mainPet.getLevel(), subPet.getMc(), subPet.getLevel(), addMc));
            newPet.setHits(getPa(template.getHits() != null ? template.getHits() : 100, mainPet.getHits(), mainPet.getLevel(), subPet.getHits(), subPet.getLevel(), addHit));
            newPet.setMiss(getPa(template.getMiss() != null ? template.getMiss() : 0, mainPet.getMiss(), mainPet.getLevel(), subPet.getMiss(), subPet.getLevel(), addMiss));
            newPet.setSpeed(getPa(template.getSpeed() != null ? template.getSpeed() : 10, mainPet.getSpeed(), mainPet.getLevel(), subPet.getSpeed(), subPet.getLevel(), addSpeed));
            long baseHp = template.getHp() != null ? template.getHp() : 100;
            long newHp = getPa(baseHp, mainPet.getSrchp(), mainPet.getLevel(), subPet.getSrchp(), subPet.getLevel(), addHp);
            newPet.setSrchp(newHp); newPet.setHp(newHp);
            long baseMp = template.getMp() != null ? template.getMp() : 50;
            long newMp = getPa(baseMp, mainPet.getSrcmp(), mainPet.getLevel(), subPet.getSrcmp(), subPet.getLevel(), addMp);
            newPet.setSrcmp(newMp); newPet.setMp(newMp);
            newPet.setAddhp(0L); newPet.setAddmp(0L);
            setCzl(newPet, newCzl);

            // Copy growth attrs from template
            newPet.setSubyl(template.getSubyl()); newPet.setSubsl(template.getSubsl());
            newPet.setSubxl(template.getSubxl()); newPet.setSubdl(template.getSubdl());
            newPet.setSubfl(template.getSubfl()); newPet.setSubhl(template.getSubhl());
            newPet.setSubkl(template.getSubkl());

            UserPet saved = userPetRepo.save(newPet);

            // PHP: learn skills from template
            learnSkillsFromTemplate(saved, template);

            // PHP: set as main pet
            player.setMbid(saved.getId().intValue());
            player.setFightBb(saved.getId().intValue());

            // Delete old pets
            deletePet(mainPetId);
            deletePet(subPetId);

            // Reset lucky star
            ext.setMergeCount(0);
            playerExtRepo.save(ext);

            r.put("code", 5); r.put("message", "合成成功！获得 " + saved.getName());
            r.put("newPetId", saved.getId()); r.put("czl", newCzl);
        } else {
            // PHP: failure — delete sub pet (unless protected)
            boolean protect = shbb1 || shbb2;
            if (!protect) deletePet(subPetId);

            ext.setMergeCount(luckyStar + 1);
            playerExtRepo.save(ext);

            r.put("code", 6);
            r.put("message", protect ? "合成失败，道具保护了副宠" : "合成失败，副宠消失，幸运星+" + (luckyStar + 1));
        }

        player.setMoney(player.getMoney() - 50000);
        playerRepo.save(player);
        cooldownService.recordOp(playerId, CooldownService.Op.COMPOSE);
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 3: 涅磐 — zsGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> nirvana(Long playerId, Long mainPetId, Long subPetId, Long beastId,
                                       Long item1Id, Long item2Id) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.NIRVANA);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet mainPet = userPetRepo.findById(mainPetId).orElse(null);
        UserPet subPet = userPetRepo.findById(subPetId).orElse(null);
        UserPet beast = userPetRepo.findById(beastId).orElse(null);
        if (mainPet == null || subPet == null || beast == null) return Map.of("code", 10, "message", "宠物不存在");
        if (!mainPet.getPlayerId().equals(playerId) || !subPet.getPlayerId().equals(playerId)
            || !beast.getPlayerId().equals(playerId))
            return Map.of("code", 10, "message", "不是你的宠物");
        if (mainPet.getId().equals(subPet.getId())) return Map.of("code", 10, "message", "主副宠不能相同");

        // PHP: wx>6 check
        if ((mainPet.getWx() != null && mainPet.getWx() > 6)
            || (subPet.getWx() != null && subPet.getWx() > 6)
            || (beast.getWx() != null && beast.getWx() > 6))
            return Map.of("code", 4, "message", "五行属于：金、木、水、火、土、神的才可以进行此操作！");

        if (mainPet.getLevel() < 60 || subPet.getLevel() < 60 || beast.getLevel() < 60)
            return Map.of("code", 4, "message", "三只宠物均需60级");

        // PHP: check extracted (also checks wx=6 czl=1 pets)
        if (isExtracted(playerId, mainPetId) || isExtracted(playerId, subPetId))
            return Map.of("code", 4, "message", "某个宠物抽取过成长,不能进行涅槃!");

        // PHP: beast must be 涅磐兽 and muchang=0
        String beastName = beast.getName();
        if (beastName == null || (!beastName.equals("涅磐兽（亥）") && !beastName.equals("涅磐兽（午）") && !beastName.equals("涅磐兽（卯）")))
            return Map.of("code", 7, "message", "请选择涅磐兽");
        if (beast.getMuchang() != null && beast.getMuchang() != 0)
            return Map.of("code", 7, "message", "涅磐兽必须是携带状态");

        // PHP: equipment check
        if ((mainPet.getZb() != null && !mainPet.getZb().isEmpty() && !mainPet.getZb().equals("0"))
            || (subPet.getZb() != null && !subPet.getZb().isEmpty() && !subPet.getZb().equals("0"))
            || (beast.getZb() != null && !beast.getZb().isEmpty() && !beast.getZb().equals("0")))
            return Map.of("code", 1000, "message", "请先卸下宠物装备");

        // PHP: template by name
        Pet mainTmpl = petRepo.findByName(mainPet.getName()).orElse(null);
        Pet subTmpl = petRepo.findByName(subPet.getName()).orElse(null);
        if (mainTmpl == null || subTmpl == null) return Map.of("code", 10, "message", "宠物模板数据异常");

        // PHP: zs table lookup
        Zs zsFormula = zsRepo.findByAidAndBid(mainTmpl.getId().intValue(), subTmpl.getId().intValue()).orElse(null);
        if (zsFormula == null) return Map.of("code", 2, "message", "不能合成");

        // PHP: item parsing
        Props pp1 = item1Id != null && item1Id > 0 ? getPropsByBagId(playerId, item1Id) : null;
        // p2: varyname=19 item with addcz:N% effect
        UserBag p2Bag = item2Id != null && item2Id > 0 ? bagRepo.findById(item2Id).orElse(null) : null;
        Props pp2 = null;
        double addczFromP2 = 0;
        if (p2Bag != null && p2Bag.getPlayerId().equals(playerId)) {
            pp2 = propsRepo.findById(p2Bag.getPropId().longValue()).orElse(null);
            if (pp2 != null && pp2.getEffect() != null) {
                String[] parts = pp2.getEffect().split(":");
                if (parts.length >= 2 && parts[0].equals("addcz")) {
                    addczFromP2 = Double.parseDouble(parts[1].replace("%", "")) * 0.01;
                }
            }
        }

        // PHP: pp1 must be varyname=8
        if (pp1 != null && (pp1.getVaryname() == null || pp1.getVaryname() != 8))
            return Map.of("code", 200, "message", "请放入涅磐道具");

        double npcgBonus = 0, npczBonus = 0, npbbProtect = 0; // npbb: 1=protect
        if (pp1 != null && pp1.getEffect() != null) {
            // effect format: "npbb:1,npcg:3000%,npcz:15%"
            for (String part : pp1.getEffect().split(",")) {
                String[] kv = part.split(":");
                if (kv.length >= 2) {
                    switch (kv[0]) {
                        case "npbb": npbbProtect = Double.parseDouble(kv[1]); break;
                        case "npcg": npcgBonus = Double.parseDouble(kv[1].replace("%", "")) / 100.0; break;
                        case "npcz": npczBonus = Double.parseDouble(kv[1].replace("%", "")) / 100.0; break;
                    }
                }
            }
        }
        // PHP: strpos(effect, 'npbb') === false → die('200')
        if (pp1 != null && pp1.getEffect() != null && !pp1.getEffect().contains("npbb"))
            return Map.of("code", 200, "message", "请放入正确的涅磐道具（需要含npbb效果）");

        // PHP: gold 500000
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < 500000))
            return Map.of("code", 3, "message", "金币不足，需要500000");

        // PHP: beast lv factor
        double lv;
        if ("涅磐兽（卯）".equals(beastName)) lv = 0.3;
        else if ("涅磐兽（午）".equals(beastName)) lv = 0.15;
        else lv = 0.05;

        // PHP: success rate = round((mainLvl/30 + subLvl/30)*(1+npcg), 2)
        double sus = round2((mainPet.getLevel() / 30.0 + subPet.getLevel() / 30.0) * (1 + npcgBonus));
        boolean success = rng().nextInt(1, 10001) <= (int)(sus * 100);

        // Consume items
        if (item1Id != null && item1Id > 0) consumeBagItemById(playerId, item1Id);
        if (p2Bag != null) consumeBagItem(p2Bag);

        Map<String, Object> r = new LinkedHashMap<>();

        if (success) {
            // PHP: bbczl() nirvana
            double mainCzl = czl(mainPet), subCzl = czl(subPet);
            double[] nums = getZsNums(mainPet.getName(), mainCzl);
            double num1 = nums[0], num2 = nums[1];

            // PHP: $czl = $a['czl'] + round(((($a['level']/$a['czl']/$num1)+($b['level']*$b['czl']/$num2))*(1+$lv+$pp1+$pp2)),1)
            double newCzl = mainCzl + round1(
                ((mainPet.getLevel() / mainCzl / num1) + (subPet.getLevel() * subCzl / num2))
                * (1 + lv + npczBonus + addczFromP2)
            );

            // PHP: create new pet from zs formula mid
            Pet template = petRepo.findById(zsFormula.getMid().longValue()).orElse(null);
            if (template == null) return Map.of("code", 10, "message", "涅磐模板不存在");

            UserPet newPet = new UserPet();
            newPet.setPlayerId(playerId);
            newPet.setUsername(player.getNickname());
            newPet.setName(template.getName());
            newPet.setLevel(1);
            newPet.setWx(template.getWx());
            newPet.setImgstand(template.getImgstand());
            newPet.setImgack(template.getImgack());
            newPet.setImgdie(template.getImgdie());
            newPet.setHeadimg("t" + template.getId() + ".gif");
            newPet.setCardimg("k" + template.getId() + ".gif");
            newPet.setEffectimg("q" + template.getId() + ".gif");
            newPet.setSkillList(template.getSkillList());
            newPet.setStime(System.currentTimeMillis() / 1000);
            newPet.setNowexp(0L);
            newPet.setLexp(100L);
            newPet.setKx(template.getKx());
            newPet.setRemakelevel(template.getRemakeLevel());
            newPet.setRemakeid(template.getRemakeId());
            newPet.setRemakepid(template.getRemakePid());
            newPet.setMuchang(0);
            newPet.setRemaketimes(0);
            newPet.setOldBid(template.getId().intValue());

            // PHP: getPa() for attrs — p2 may have addac/addmc/addhits/addhp
            double pac = 0, pmc = 0, phits = 0, php = 0;
            if (pp2 != null && pp2.getEffect() != null) {
                String[] parts = pp2.getEffect().split(":");
                if (parts.length >= 2) {
                    switch (parts[0]) {
                        case "addac": pac = Double.parseDouble(parts[1].replace("%", "")) * 0.01; break;
                        case "addmc": pmc = Double.parseDouble(parts[1].replace("%", "")) * 0.01; break;
                        case "addhits": phits = Double.parseDouble(parts[1].replace("%", "")) * 0.01; break;
                        case "addhp": php = Double.parseDouble(parts[1].replace("%", "")) * 0.01; break;
                    }
                }
            }

            newPet.setAc(getPa(template.getAc() != null ? template.getAc() : 10, mainPet.getAc(), mainPet.getLevel(), subPet.getAc(), subPet.getLevel(), pac));
            newPet.setMc(getPa(template.getMc() != null ? template.getMc() : 10, mainPet.getMc(), mainPet.getLevel(), subPet.getMc(), subPet.getLevel(), pmc));
            newPet.setHits(getPa(template.getHits() != null ? template.getHits() : 100, mainPet.getHits(), mainPet.getLevel(), subPet.getHits(), subPet.getLevel(), phits));
            newPet.setMiss(getPa(template.getMiss() != null ? template.getMiss() : 0, mainPet.getMiss(), mainPet.getLevel(), subPet.getMiss(), subPet.getLevel(), 0));
            newPet.setSpeed(getPa(template.getSpeed() != null ? template.getSpeed() : 10, mainPet.getSpeed(), mainPet.getLevel(), subPet.getSpeed(), subPet.getLevel(), 0));
            long nirvHp = getPa(template.getHp() != null ? template.getHp() : 100, mainPet.getSrchp(), mainPet.getLevel(), subPet.getSrchp(), subPet.getLevel(), php);
            newPet.setSrchp(nirvHp); newPet.setHp(nirvHp);
            long nirvMp = getPa(template.getMp() != null ? template.getMp() : 50, mainPet.getSrcmp(), mainPet.getLevel(), subPet.getSrcmp(), subPet.getLevel(), 0);
            newPet.setSrcmp(nirvMp); newPet.setMp(nirvMp);
            newPet.setAddhp(0L); newPet.setAddmp(0L);
            setCzl(newPet, newCzl);

            newPet.setSubyl(template.getSubyl()); newPet.setSubsl(template.getSubsl());
            newPet.setSubxl(template.getSubxl()); newPet.setSubdl(template.getSubdl());
            newPet.setSubfl(template.getSubfl()); newPet.setSubhl(template.getSubhl());
            newPet.setSubkl(template.getSubkl());

            UserPet saved = userPetRepo.save(newPet);
            learnSkillsFromTemplate(saved, template);
            player.setMbid(saved.getId().intValue());
            player.setFightBb(saved.getId().intValue());

            deletePet(mainPetId);
            deletePet(subPetId);
            deletePet(beastId);

            r.put("code", 5); r.put("message", "涅磐成功！获得 " + saved.getName());
            r.put("newPetId", saved.getId()); r.put("czl", newCzl);
        } else {
            // PHP: beast dies unless npbb protect
            if (npbbProtect != 1) deletePet(beastId);
            r.put("code", 6);
            r.put("message", npbbProtect == 1 ? "涅磐失败，守护道具保护了涅磐兽" : "涅磐失败，涅磐兽消失");
        }

        player.setMoney(player.getMoney() - 500000);
        playerRepo.save(player);
        cooldownService.recordOp(playerId, CooldownService.Op.NIRVANA);
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 4: 神圣进化 — superJhGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> sacredEvolve(Long playerId, Long petId, Long itemId) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.SACRED_EVOLVE);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || !pet.getPlayerId().equals(playerId)) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: if($bb['wx']!=7) die('请确认您的宠物是否为神圣宠物！')
        if (pet.getWx() == null || pet.getWx() != 7)
            return Map.of("code", 4, "message", "请确认您的宠物是否为神圣宠物！");

        int times = pet.getRemaketimes() != null ? pet.getRemaketimes() : 0;
        if (times >= 10) return Map.of("code", 4, "message", "您的宠物已经达到该阶段进化上限，无法再进化了！");

        // PHP: template by name
        Pet bbO = petRepo.findByName(pet.getName()).orElse(null);
        if (bbO == null) return Map.of("code", 10, "message", "内存中找不到要进化的宠物的原始数据！");

        SuperJh jhConfig = superJhRepo.findByPetId(bbO.getId().intValue()).orElse(null);
        if (jhConfig == null) return Map.of("code", 4, "message", "数据库中没有该宠物神圣进化的设定！");

        // PHP: need_levels indexed by remaketimes
        String needLevels = jhConfig.getNeedLevels();
        if (needLevels == null || needLevels.isEmpty()) return Map.of("code", 10, "message", "进化配置异常");
        String[] nlvs = needLevels.split(",");
        int limitLvl = times < nlvs.length ? Integer.parseInt(nlvs[times].trim()) : Integer.parseInt(nlvs[0].trim());
        if (pet.getLevel() < limitLvl) return Map.of("code", 4, "message", "宠物等级(" + limitLvl + ")不够，请先升级宠物！");

        // PHP: need_props indexed by remaketimes
        String needProps = jhConfig.getNeedProps();
        String[] nprops = needProps != null ? needProps.split(",") : new String[0];
        String npropsIds = times < nprops.length ? nprops[times].trim() : (nprops.length > 0 ? nprops[0].trim() : "");

        // PHP: gold = (zs_progress + remaketimes) * 10000
        int gold = (jhConfig.getZsProgress() != null ? jhConfig.getZsProgress() : 1 + times) * 10000;
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < gold))
            return Map.of("code", 3, "message", "金币不足，需要" + gold);

        // PHP: zjsxdj item (optional attribute bonus)
        double zjsxBonusHp = 0, zjsxBonusMp = 0, zjsxBonusAc = 0, zjsxBonusMc = 0,
               zjsxBonusSpeed = 0, zjsxBonusHits = 0, zjsxBonusMiss = 0;
        if (itemId != null && itemId > 0) {
            UserBag zjBag = bagRepo.findById(itemId).orElse(null);
            if (zjBag != null && zjBag.getPlayerId().equals(playerId)) {
                Props zjProps = propsRepo.findById(zjBag.getPropId().longValue()).orElse(null);
                if (zjProps != null && zjProps.getEffect() != null) {
                    // effect format: "zjsxdj_<attr>:N"
                    String[] parts = zjProps.getEffect().split(":");
                    if (parts.length >= 2) {
                        String attrKey = parts[0].replace("zjsxdj_", "");
                        double bonus = Double.parseDouble(parts[1].replaceAll("[^0-9]", "")) / 100.0;
                        switch (attrKey) {
                            case "hp": zjsxBonusHp = bonus; break;
                            case "mp": zjsxBonusMp = bonus; break;
                            case "ac": zjsxBonusAc = bonus; break;
                            case "mc": zjsxBonusMc = bonus; break;
                            case "speed": zjsxBonusSpeed = bonus; break;
                            case "hits": zjsxBonusHits = bonus; break;
                            case "miss": zjsxBonusMiss = bonus; break;
                        }
                    }
                }
                consumeBagItem(zjBag);
            }
        }

        // PHP: process required materials and ssjh growth items
        double perCzl = 0;
        if (!npropsIds.isEmpty() && !npropsIds.equals("0")) {
            for (String entry : npropsIds.split("\\|")) {
                String[] parts = entry.split(":");
                if (parts.length < 2) continue;
                int propId = Math.abs(Integer.parseInt(parts[0].trim()));
                int count = Math.abs(Integer.parseInt(parts[1].trim()));

                // Check and consume this material
                UserBag matBag = bagRepo.findByPlayerId(playerId).stream()
                    .filter(b -> b.getPropId() != null && b.getPropId().intValue() == propId
                        && (b.getSums() != null && b.getSums() >= count))
                    .findFirst().orElse(null);
                if (matBag == null) return Map.of("code", 2, "message", "物品不足！");

                // PHP: check if material has ssjh:min|max effect
                Props matProps = propsRepo.findById((long) propId).orElse(null);
                if (matProps != null && matProps.getEffect() != null && matProps.getEffect().contains("ssjh:")) {
                    String ssjhStr = matProps.getEffect().replace("ssjh:", "");
                    String[] chance = ssjhStr.split("\\|");
                    double min = Double.parseDouble(chance[0]) * 100;
                    double max = Double.parseDouble(chance[1]) * 100;
                    perCzl = rng().nextDouble(min, max + 1); // rand(min*100, max*100)
                }

                // Consume
                int rem = matBag.getSums();
                if (rem <= count) bagRepo.delete(matBag);
                else { matBag.setSums(rem - count); bagRepo.save(matBag); }
            }
        }

        // PHP: growth = czl + perCzl/100
        double currentCzl = czl(pet);
        double newCzl = currentCzl + perCzl / 100.0;
        int maxCzl = jhConfig.getMaxCzl() != null ? jhConfig.getMaxCzl() : 150;
        if (newCzl > maxCzl) newCzl = maxCzl;
        newCzl = round1(newCzl);

        // PHP: wx_sx table lookup for attribute multipliers
        Wx wxSx = wxRepo.findByWx(pet.getWx());
        if (wxSx == null) return Map.of("code", 10, "message", "查找宠物五行设定失败！");

        int zsProgress = jhConfig.getZsProgress() != null ? jhConfig.getZsProgress() : 1;
        int nextTimes = times + 1;

        // PHP: attr * (0.3 + (remaketimes+1)/30 + (remaketimes+1)*zs_progress/(czl*wx_sx[attr]))
        long newAc = Math.round(pet.getAc() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getAc())));
        long newMc = Math.round(pet.getMc() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getMc())));
        long newHp = Math.round(pet.getSrchp() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getHp())));
        long newMp = Math.round(pet.getSrcmp() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getMp())));
        long newSpeed = Math.round(pet.getSpeed() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getSpeed())));
        long newHits = Math.round(pet.getHits() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getHits())));
        long newMiss = Math.round(pet.getMiss() * (0.3 + nextTimes / 30.0 + nextTimes * zsProgress / (currentCzl * wxSx.getMiss())));

        // PHP: apply zjsxdj bonuses
        newAc = Math.round(newAc * (1 + zjsxBonusAc));
        newMc = Math.round(newMc * (1 + zjsxBonusMc));
        newHp = Math.round(newHp * (1 + zjsxBonusHp));
        newMp = Math.round(newMp * (1 + zjsxBonusMp));
        newSpeed = Math.round(newSpeed * (1 + zjsxBonusSpeed));
        newHits = Math.round(newHits * (1 + zjsxBonusHits));
        newMiss = Math.round(newMiss * (1 + zjsxBonusMiss));

        pet.setAc(newAc);
        pet.setMc(newMc);
        pet.setSrchp(newHp); pet.setHp(newHp);
        pet.setSrcmp(newMp); pet.setMp(newMp);
        pet.setSpeed(newSpeed);
        pet.setHits(newHits);
        pet.setMiss(newMiss);
        pet.setLevel(1);
        pet.setLexp(100L);
        pet.setNowexp(0L);
        pet.setRemaketimes(nextTimes);
        setCzl(pet, newCzl);

        userPetRepo.save(pet);

        player.setMoney(player.getMoney() - gold);
        playerRepo.save(player);
        cooldownService.recordOp(playerId, CooldownService.Op.SACRED_EVOLVE);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 1); r.put("message", "神圣进化成功");
        r.put("czl", newCzl); r.put("remaketimes", nextTimes);
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 4: 成长抽取 — cqGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> extractGrowth(Long playerId, Long petId, Long item1Id, Long item2Id) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.EXTRACT_GROWTH);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || !pet.getPlayerId().equals(playerId)) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: if($bb['wx']>6) die('该宠物不能抽取!')
        int wx = pet.getWx() != null ? pet.getWx() : 1;
        if (wx > 6) return Map.of("code", 4, "message", "该宠物不能抽取!");

        if (isExtracted(playerId, petId))
            return Map.of("code", 4, "message", "这个宠物抽取过成长,不能再抽取!");

        double currentCzl = czl(pet);
        if (currentCzl < 30) return Map.of("code", 15, "message", "成长小于30的不能抽取！");

        // PHP: item checks
        Props p1Props = item1Id != null && item1Id > 0 ? getPropsByBagId(playerId, item1Id) : null;
        Props p2Props = item2Id != null && item2Id > 0 ? getPropsByBagId(playerId, item2Id) : null;

        int p1Pid = p1Props != null && p1Props.getId() != null ? p1Props.getId().intValue() : 0;
        int p2Pid = p2Props != null && p2Props.getId() != null ? p2Props.getId().intValue() : 0;

        // PHP: wx<6 must have item 3383; wx=6 must NOT have item 3383
        if (wx < 6 && p1Pid != 3383 && p2Pid != 3383)
            return Map.of("code", 2, "message", "缺少五系宠物抽取的必须道具！");
        if (wx > 5 && (p1Pid == 3383 || p2Pid == 3383))
            return Map.of("code", 4, "message", "非五系宠物不能使用五系宠物抽取石！");
        if (wx < 6 && (p1Pid != 3383 && p1Pid != 0 || p2Pid != 3383 && p2Pid != 0)) {
            // Actually check: p1 and p2 for wx<6 must be 3383 if not 0
            boolean p1ok = item1Id == null || item1Id == 0 || p1Pid == 3383;
            boolean p2ok = item2Id == null || item2Id == 0 || p2Pid == 3383;
            if (!p1ok || !p2ok) return Map.of("code", 4, "message", "五系宠物不能使用增加比例道具！");
        }
        if (item1Id != null && item1Id.equals(item2Id) && item1Id > 0 && p1Pid == 3383)
            return Map.of("code", 4, "message", "请不要使用两个五系宠物抽取石！");

        // PHP: parse inczhl bonuses
        int swapRateInc = 0, swapRateIncFixed = 0;
        if (p1Props != null && p1Props.getEffect() != null && p1Props.getEffect().contains("inczhl:")) {
            String str = p1Props.getEffect().replace("inczhl:", "");
            if (!str.contains("a")) swapRateInc += Math.abs(Integer.parseInt(str));
            else swapRateIncFixed += Math.abs(Integer.parseInt(str.replace("a", "")));
            consumeBagItemById(playerId, item1Id);
            // PHP: same item used twice?
            if (item1Id != null && item1Id.equals(item2Id)) {
                if (!str.contains("a")) swapRateInc += Math.abs(Integer.parseInt(str));
                else swapRateIncFixed += Math.abs(Integer.parseInt(str.replace("a", "")));
                // already consumed once, but need to consume second count
                consumeBagItemById(playerId, item1Id);
            }
        }
        if (p2Props != null && p2Props.getEffect() != null && p2Props.getEffect().contains("inczhl:")
            && (item1Id == null || !item1Id.equals(item2Id))) {
            String str = p2Props.getEffect().replace("inczhl:", "");
            if (!str.contains("a")) swapRateInc += Math.abs(Integer.parseInt(str));
            else swapRateIncFixed += Math.abs(Integer.parseInt(str.replace("a", "")));
            consumeBagItemById(playerId, item2Id);
        }

        // PHP: swapRate based on czl ranges (spirit wx=6)
        int swapRate;
        if (wx == 6) {
            if (currentCzl < 65) swapRate = rng().nextInt(10, 21);
            else if (currentCzl < 85) swapRate = rng().nextInt(30, 51);
            else if (currentCzl < 100) swapRate = rng().nextInt(50, 66);
            else if (currentCzl < 110) swapRate = 65;
            else if (currentCzl < 115) swapRate = 70;
            else if (currentCzl < 120) swapRate = 75;
            else swapRate = 80;
        } else {
            // PHP: 五行宠物 rand(5,15)
            swapRate = rng().nextInt(5, 16);
        }

        swapRate += swapRateInc;
        if (swapRate > 100) swapRate = 100;

        // PHP: cap effective czl for extraction at 600
        double effectiveCzl = currentCzl;
        if (currentCzl * (swapRate / 100.0) > 600) {
            effectiveCzl = 600.0 / swapRate * 100;
        }

        // PHP: gold = min(czl*10000, 6000000)
        int gold = (int) Math.min(currentCzl * 10000, 6000000);

        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < gold))
            return Map.of("code", 3, "message", "金币不足，需要" + gold);

        // PHP: extracted = ceil(czl * swapRate/100) + fixed
        int extracted = (int) Math.ceil(effectiveCzl * swapRate / 100.0) + swapRateIncFixed;
        if (extracted > 600) extracted = 600;

        // PHP: save to czl_ss
        PlayerExt ext = getOrCreatePlayerExt(playerId.intValue());
        int currentSs = ext.getCzlSs() != null ? ext.getCzlSs() : 0;
        ext.setCzlSs(currentSs + extracted);

        // PHP: mark as extracted
        String existing = ext.getChouquChongwu() != null ? ext.getChouquChongwu() : ",";
        ext.setChouquChongwu(existing + petId + ",");
        playerExtRepo.save(ext);

        Map<String, Object> r = new LinkedHashMap<>();
        if (wx < 6) {
            // PHP: 五行宠物 — set uid=0, name=concat(name,"-",uid)
            pet.setPlayerId(0L);
            pet.setName(pet.getName() + "-" + playerId);
            userPetRepo.save(pet);
            r.put("code", 1); r.put("message", "抽取成功，宠物已消失");
        } else {
            // PHP: 神宠 — czl=1
            setCzl(pet, 1.0);
            userPetRepo.save(pet);
            r.put("code", 1); r.put("message", "抽取成功，宠物成长重置为1");
        }

        player.setMoney(player.getMoney() - gold);
        playerRepo.save(player);
        cooldownService.recordOp(playerId, CooldownService.Op.EXTRACT_GROWTH);

        r.put("extracted", extracted); r.put("totalSs", ext.getCzlSs());
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 4: 成长转化 — zhGate.php
    // ═══════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> convertGrowth(Long playerId, Long petId, int value) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.CONVERT_GROWTH);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || !pet.getPlayerId().equals(playerId)) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: if($bb['wx']!=7) die('这个宠物不能接受转化！')
        if (pet.getWx() == null || pet.getWx() != 7)
            return Map.of("code", 4, "message", "这个宠物不能接受转化！");

        PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
        int czlSs = ext != null && ext.getCzlSs() != null ? ext.getCzlSs() : 0;
        if (value > czlSs) return Map.of("code", 4, "message", "剩余成长不够！");

        // PHP: get max_czl from super_jh
        Pet bbO = petRepo.findByName(pet.getName()).orElse(null);
        if (bbO == null) return Map.of("code", 10, "message", "内存中找不到宠物的原始数据！");

        SuperJh jhConfig = superJhRepo.findByPetId(bbO.getId().intValue()).orElse(null);
        if (jhConfig == null) return Map.of("code", 4, "message", "数据库中没有该宠物神圣进化的设定！");

        int maxCzl = jhConfig.getMaxCzl() != null ? jhConfig.getMaxCzl() : 150;
        double currentCzl = czl(pet);

        // PHP: if value+czl > max_czl, value = max_czl - czl
        int actual = value;
        if (value + currentCzl > maxCzl) {
            actual = (int)(maxCzl - currentCzl);
        }
        if (actual <= 0) return Map.of("code", 4, "message", "成长已达上限(" + maxCzl + ")");

        // PHP: consume from czl_ss (uses original value, not capped)
        ext.setCzlSs(czlSs - Math.min(value, czlSs)); // Actually PHP uses ceil(value)
        playerExtRepo.save(ext);

        setCzl(pet, currentCzl + actual);
        userPetRepo.save(pet);

        cooldownService.recordOp(playerId, CooldownService.Op.CONVERT_GROWTH);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 1); r.put("message", "转化成功");
        r.put("added", actual); r.put("czl", czl(pet)); r.put("remainingSs", ext.getCzlSs());
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Tab 5: 神圣转生 — sszsInfo.php
    // ═══════════════════════════════════════════════════

    /** op=img */
    public List<Map<String, Object>> getRebirthImages(Long petId) {
        UserPet userPet = userPetRepo.findById(petId).orElse(null);
        if (userPet == null || userPet.getOldBid() == null) return List.of();
        Integer petTemplateId = userPet.getOldBid();
        List<SuperZs> list = superZsRepo.findByCurPetId(petTemplateId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SuperZs zs : list) {
            String nextIds = zs.getNextPetId();
            if (nextIds == null || nextIds.isEmpty()) continue;
            for (String idStr : nextIds.split(",")) {
                int tid = Integer.parseInt(idStr.trim());
                Pet template = petRepo.findById((long) tid).orElse(null);
                if (template != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("zsId", zs.getId());
                    m.put("petId", tid);
                    m.put("name", template.getName());
                    m.put("img", template.getImgstand());
                    m.put("cardImg", template.getCardimg());
                    result.add(m);
                }
            }
        }
        return result;
    }

    /** op=str */
    public Map<String, Object> getRebirthInfo(Integer zsId, Long petId) {
        SuperZs zs = superZsRepo.findById(zsId).orElse(null);
        if (zs == null) return Map.of("code", 10, "message", "转生配置不存在");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: gold = zs_progress of NEXT pet * 100000
        int goldCost = 0;
        String nextIds = zs.getNextPetId();
        if (nextIds != null && !nextIds.isEmpty()) {
            int firstNextId = Integer.parseInt(nextIds.split(",")[0].trim());
            SuperJh nextJh = superJhRepo.findByPetId(firstNextId).orElse(null);
            int zsProgress = nextJh != null && nextJh.getZsProgress() != null ? nextJh.getZsProgress() : 1;
            goldCost = zsProgress * 100000;
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("needLevel", zs.getNeedLevel());
        r.put("needCzl", zs.getNeedCzl());
        r.put("needProps", zs.getNeedProps());
        r.put("baseSuccessRate", zs.getBaseSuccessRate());
        r.put("goldCost", goldCost);

        if (nextIds != null && !nextIds.isEmpty()) {
            List<Map<String, Object>> nextPets = new ArrayList<>();
            for (String idStr : nextIds.split(",")) {
                Pet t = petRepo.findById((long) Integer.parseInt(idStr.trim())).orElse(null);
                if (t != null) {
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("id", t.getId()); pm.put("name", t.getName());
                    pm.put("img", t.getImgstand());
                    nextPets.add(pm);
                }
            }
            r.put("nextPets", nextPets);
        }
        return r;
    }

    /** op=zs */
    @Transactional
    public Map<String, Object> sacredRebirth(Long playerId, Long petId, Integer zsId,
                                              Long item1Id, Long item2Id) {
        long cd = cooldownService.checkCooldown(playerId, CooldownService.Op.SACRED_REBIRTH);
        if (cd > 0) return Map.of("code", 11, "message", "冷却中，" + cd + "ms");

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || !pet.getPlayerId().equals(playerId)) return Map.of("code", 10, "message", "宠物不存在");

        // PHP: if($bb['wx'] != 7) die('该宠物非神圣宠物，不能使用此功能！')
        if (pet.getWx() == null || pet.getWx() != 7)
            return Map.of("code", 4, "message", "该宠物非神圣宠物，不能使用此功能！");

        // PHP: resolve zs entry
        SuperZs need;
        if (zsId > 0) {
            need = superZsRepo.findById(zsId).orElse(null);
        } else {
            // Look up by pet name → bb template → cur_pet_id
            Pet bbTmpl = petRepo.findByName(pet.getName()).orElse(null);
            if (bbTmpl == null) return Map.of("code", 10, "message", "宠物模板数据异常");
            List<SuperZs> list = superZsRepo.findByCurPetId(bbTmpl.getId().intValue());
            need = list.isEmpty() ? null : list.get(0);
        }
        if (need == null) return Map.of("code", 10, "message", "转生配置不存在");

        // PHP: check requirements
        if (need.getNeedLevel() != null && pet.getLevel() < need.getNeedLevel())
            return Map.of("code", 4, "message", "等级不足" + need.getNeedLevel());
        double currentCzl = czl(pet);
        if (need.getNeedCzl() != null && currentCzl < need.getNeedCzl())
            return Map.of("code", 15, "message", "成长不足" + need.getNeedCzl());

        // PHP: check required materials
        String needProps = need.getNeedProps();
        if (needProps != null && !needProps.isEmpty()) {
            for (String group : needProps.split(",")) {
                String[] parts = group.split("\\|");
                if (parts.length >= 2) {
                    int pid = Integer.parseInt(parts[0].trim());
                    int cnt = Integer.parseInt(parts[1].trim());
                    if (countProp(playerId, pid) < cnt)
                        return Map.of("code", 2, "message", "相应的必须品不够！");
                }
            }
        }

        // PHP: check same item twice has enough count
        if (item1Id != null && item1Id.equals(item2Id) && item1Id > 0) {
            UserBag bag = bagRepo.findById(item1Id).orElse(null);
            if (bag == null || bag.getSums() == null || bag.getSums() < 2)
                return Map.of("code", 2, "message", "道具不足！");
        }

        // PHP: parse item effects
        Props wp1Props = item1Id != null && item1Id > 0 ? getPropsByBagId(playerId, item1Id) : null;
        Props wp2Props = item2Id != null && item2Id > 0 ? getPropsByBagId(playerId, item2Id) : null;

        double sszsBonus = 0;
        double czlnum = 10;
        String attrEff = null;

        if (wp1Props != null && wp1Props.getEffect() != null) {
            String eff = wp1Props.getEffect();
            if (eff.contains("sszs")) {
                sszsBonus += Double.parseDouble(eff.replace("sszs:", "")) / 100.0;
            } else if (eff.contains("cszsczlbh")) {
                czlnum += Double.parseDouble(eff.replace("cszsczlbh:", ""));
            } else {
                attrEff = eff; // e.g., "addac:15" or "addhp:20"
            }
        }
        if (wp2Props != null && wp2Props.getEffect() != null) {
            String eff = wp2Props.getEffect();
            if (eff.contains("sszs")) {
                sszsBonus += Double.parseDouble(eff.replace("sszs:", "")) / 100.0;
            } else if (eff.contains("cszsczlbh")) {
                czlnum += Double.parseDouble(eff.replace("cszsczlbh:", ""));
            } else if (attrEff == null) {
                attrEff = eff;
            }
        }

        // PHP: gold = zs_progress of NEXT pet * 100000
        String nextIds = need.getNextPetId();
        if (nextIds == null || nextIds.isEmpty()) return Map.of("code", 10, "message", "转生目标配置异常");
        int nextPetId = Integer.parseInt(nextIds.split(",")[0].trim());
        SuperJh nextJh = superJhRepo.findByPetId(nextPetId).orElse(null);
        int zsjd = nextJh != null && nextJh.getZsProgress() != null ? nextJh.getZsProgress() : 1;
        int gold = zsjd * 100000;

        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player == null || (player.getMoney() != null && player.getMoney() < gold))
            return Map.of("code", 3, "message", "金币不足" + gold);

        // PHP: success rate = round((level/30*(1+sszs_bonus)),2)*100
        double sus = round2((pet.getLevel() / 30.0) * (1 + sszsBonus));
        boolean success = rng().nextInt(1, 10001) <= (int)(sus * 100);

        // Consume items
        if (item1Id != null && item1Id > 0) consumeBagItemById(playerId, item1Id);
        if (item2Id != null && item2Id > 0) consumeBagItemById(playerId, item2Id);

        // Consume required materials
        if (needProps != null && !needProps.isEmpty()) {
            for (String group : needProps.split(",")) {
                String[] parts = group.split("\\|");
                if (parts.length >= 2) {
                    consumePropCount(playerId, Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                }
            }
        }

        player.setMoney(player.getMoney() - gold);
        playerRepo.save(player);

        Map<String, Object> r = new LinkedHashMap<>();

        if (success) {
            // PHP: pick random from next_pet_id list
            String[] allNext = nextIds.split(",");
            int chosenNextId = Integer.parseInt(allNext[rng().nextInt(allNext.length)].trim());
            Pet newbb = petRepo.findById((long) chosenNextId).orElse(null);
            if (newbb == null) return Map.of("code", 10, "message", "转生模板不存在");

            // PHP: parse attribute bonus from item effect
            double pac = 0, pmc = 0, phit = 0, pmiss = 0, pspeed = 0, php = 0, pmp = 0;
            if (attrEff != null) {
                String[] parts = attrEff.split(":");
                if (parts.length >= 2) {
                    double bonus = Double.parseDouble(parts[1]) / 100.0;
                    switch (parts[0]) {
                        case "addac": pac = bonus; break;
                        case "addmc": pmc = bonus; break;
                        case "addhit": phit = bonus; break;
                        case "addmiss": pmiss = bonus; break;
                        case "addspeed": pspeed = bonus; break;
                        case "addhp": php = bonus; break;
                        case "addmp": pmp = bonus; break;
                    }
                }
            }

            // PHP: attribute = round((newBase*zsjd + oldAttr*level/6000 + oldAttr*czl/9000)*(p+1))
            long newHp = Math.round((newbb.getHp() * zsjd + pet.getHp() * pet.getLevel() / 6000.0 + pet.getHp() * currentCzl / 9000.0) * (php + 1));
            long newMp = Math.round((newbb.getMp() * zsjd + pet.getMp() * pet.getLevel() / 6000.0 + pet.getMp() * currentCzl / 9000.0) * (pmp + 1));
            long newMc = Math.round((newbb.getMc() * zsjd + pet.getMc() * pet.getLevel() / 6000.0 + pet.getMc() * currentCzl / 9000.0) * (pmc + 1));
            long newAc = Math.round((newbb.getAc() * zsjd + pet.getAc() * pet.getLevel() / 6000.0 + pet.getAc() * currentCzl / 9000.0) * (pac + 1));
            long newHits = Math.round((newbb.getHits() * zsjd + pet.getHits() * pet.getLevel() / 6000.0 + pet.getHits() * currentCzl / 9000.0) * (phit + 1));
            long newMiss = Math.round((newbb.getMiss() * zsjd + pet.getMiss() * pet.getLevel() / 6000.0 + pet.getMiss() * currentCzl / 9000.0) * (pmiss + 1));
            long newSpeed = Math.round((newbb.getSpeed() * zsjd + pet.getSpeed() * pet.getLevel() / 6000.0 + pet.getSpeed() * currentCzl / 9000.0) * (pspeed + 1));

            // PHP: czl = round(oldCzl * czlnum * 0.01, 1)
            double newCzl = round1(currentCzl * czlnum * 0.01);

            UserPet saved = new UserPet();
            saved.setPlayerId(playerId);
            saved.setUsername(player.getNickname());
            saved.setName(newbb.getName());
            saved.setLevel(1);
            saved.setWx(newbb.getWx());
            saved.setImgstand(newbb.getImgstand());
            saved.setImgack(newbb.getImgack());
            saved.setImgdie(newbb.getImgdie());
            saved.setHeadimg("t" + newbb.getId() + ".gif");
            saved.setCardimg("k" + newbb.getId() + ".gif");
            saved.setEffectimg("q" + newbb.getId() + ".gif");
            saved.setSkillList(newbb.getSkillList());
            saved.setStime(System.currentTimeMillis() / 1000);
            saved.setNowexp(0L);
            saved.setLexp(100L);
            saved.setKx(newbb.getKx());
            saved.setRemakelevel(newbb.getRemakeLevel());
            saved.setRemakeid(newbb.getRemakeId());
            saved.setRemakepid(newbb.getRemakePid());
            saved.setMuchang(0);
            saved.setRemaketimes(0);
            saved.setOldBid(newbb.getId().intValue());

            saved.setAc(newAc); saved.setMc(newMc);
            saved.setSrchp(newHp); saved.setHp(newHp);
            saved.setSrcmp(newMp); saved.setMp(newMp);
            saved.setHits(newHits); saved.setMiss(newMiss);
            saved.setSpeed(newSpeed);
            saved.setAddhp(0L); saved.setAddmp(0L);
            setCzl(saved, newCzl);
            saved.setSubyl(newbb.getSubyl()); saved.setSubsl(newbb.getSubsl());
            saved.setSubxl(newbb.getSubxl()); saved.setSubdl(newbb.getSubdl());
            saved.setSubfl(newbb.getSubfl()); saved.setSubhl(newbb.getSubhl());
            saved.setSubkl(newbb.getSubkl());

            saved = userPetRepo.save(saved);
            learnSkillsFromTemplate(saved, newbb);
            player.setMbid(saved.getId().intValue());
            player.setFightBb(saved.getId().intValue());
            playerRepo.save(player);

            deletePet(petId);

            r.put("code", 5); r.put("message", "转生成功！获得 " + saved.getName());
            r.put("newPetId", saved.getId()); r.put("czl", newCzl);
        } else {
            r.put("code", 6); r.put("message", "转生失败");
        }

        cooldownService.recordOp(playerId, CooldownService.Op.SACRED_REBIRTH);
        return r;
    }

    // ═══════════════════════════════════════════════════
    // Formula helpers — exact PHP replication
    // ═══════════════════════════════════════════════════

    /** PHP round to 1 decimal place. */
    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** PHP round to 2 decimal places. */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * PHP getPa(): intval(($old+(intval($a*$app['level']/400)+intval($b*$bpp['level']/800)))*$p)
     * where $p = 1+bonus (or 1 if bonus<=0)
     */
    private static long getPa(long old, Long a, int aLvl, Long b, int bLvl, double p) {
        long aVal = a != null ? a : 0;
        long bVal = b != null ? b : 0;
        double factor = (p <= 0) ? 1.0 : 1.0 + p;
        return (long) ((old + (long)(aVal * aLvl / 400.0) + (long)(bVal * bLvl / 800.0)) * factor);
    }

    /**
     * PHP bbczl() for compose — 5 czl ranges.
     * Note: PHP uses $a['czl'] directly as float.
     */
    private static double bbczlCompose(double mainCzl, int mainLvl, double subCzl, int subLvl, double addczlBonus) {
        if (mainCzl < 51.0)
            return round1(mainCzl + (mainLvl / (mainCzl + 10) + subLvl * subCzl / 200.0) * (1 + addczlBonus));
        if (mainCzl < 70.0 || mainCzl >= 51.0)
            return round1(mainCzl + (mainLvl / mainCzl + subLvl * subCzl / 350.0) * (1 + addczlBonus));
        if (mainCzl < 90.0 || mainCzl >= 70.0)
            return round1(mainCzl + (mainLvl / mainCzl + subLvl * subCzl / 500.0) * (1 + addczlBonus));
        if (mainCzl < 100.0 || mainCzl >= 90.0)
            return round1(mainCzl + (mainLvl / mainCzl + subLvl * subCzl / 700.0) * (1 + addczlBonus));
        return round1(mainCzl + (mainLvl / mainCzl + subLvl * subCzl / 900.0) * (1 + addczlBonus));
    }

    /** Parse compose item effect string like "hecheng:A:10%,B:4%|addczl:8%|1" */
    private static ComposeItemParsed parseComposeItem(String effect) {
        ComposeItemParsed r = new ComposeItemParsed();
        if (effect == null || effect.isEmpty()) return r;

        String[] segments = effect.split("\\|");
        for (String seg : segments) {
            if (seg.startsWith("hecheng:")) {
                // "hecheng:A:10%,B:4%"
                String inner = seg.substring("hecheng:".length());
                String[] parts = inner.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length >= 2) {
                        double val = Double.parseDouble(kv[1].replace("%", "")) / 100.0;
                        if ("A".equals(kv[0])) r.hechengA = val;
                        else if ("B".equals(kv[0])) r.hechengB = val;
                    }
                }
            } else if (seg.startsWith("addczl:")) {
                r.addczl = Double.parseDouble(seg.substring("addczl:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addac:")) {
                r.addAc = Double.parseDouble(seg.substring("addac:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addmc:")) {
                r.addMc = Double.parseDouble(seg.substring("addmc:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addhit:")) {
                r.addHit = Double.parseDouble(seg.substring("addhit:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addmiss:")) {
                r.addMiss = Double.parseDouble(seg.substring("addmiss:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addspeed:")) {
                r.addSpeed = Double.parseDouble(seg.substring("addspeed:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addhp:")) {
                r.addHp = Double.parseDouble(seg.substring("addhp:".length()).replace("%", "")) / 100.0;
            } else if (seg.startsWith("addmp:")) {
                r.addMp = Double.parseDouble(seg.substring("addmp:".length()).replace("%", "")) / 100.0;
            } else if ("1".equals(seg) || "2".equals(seg)) {
                // Last segment "1" means protect (shbb)
                if ("1".equals(seg)) r.shbb = true;
            }
        }
        return r;
    }

    private static class ComposeItemParsed {
        double hechengA, hechengB, addczl;
        double addAc, addMc, addHit, addMiss, addSpeed, addHp, addMp;
        boolean shbb;
    }

    /**
     * PHP zsGate bbczl() — get num1/num2 from zs1/zs2/zs3 name lists + czl tiers.
     * zs1/zs2/zs3 are server-configured pet name lists stored in memcache key 'db_welcome1'.
     */
    private static double[] getZsNums(String name, double czl) {
        // zs1 pet names (from PHP comments)
        Set<String> zs1 = Set.of("小神龙琅玡", "★青龙★", "★破天虎★", "白虎", "★龙蛇玄武★",
            "圣兽赤牝鹿", "蝶·影娅瑟", "尤佳娜", "GM-鸭子", "忍者小乌龟", "囧娃娃", "蜡笔妹妹", "四叶草宝宝");
        // zs2 pet names
        Set<String> zs2 = Set.of("熊猫orz宝宝", "火羽凤凰", "雪羽凤凰", "蛇女美杜莎");
        // zs3 pet names
        Set<String> zs3 = Set.of("★寒江雪★", "寒江雪宝宝", "自然女神·影", "暗夜女神·影");

        int category;
        if (zs1.contains(name)) category = 1;
        else if (zs2.contains(name)) category = 2;
        else if (zs3.contains(name)) category = 3;
        else category = 3; // default to zs3

        // czl-range tier tables matching PHP
        double num1, num2;
        if (category == 1) {
            if (czl <= 10.9)          { num1 = 1; num2 = 200; }
            else if (czl <= 30.9)     { num1 = 1; num2 = 250; }
            else if (czl <= 49.9)     { num1 = 1; num2 = 350; }
            else if (czl <= 60.9)     { num1 = 1; num2 = 480; }
            else if (czl <= 70.9)     { num1 = 1; num2 = 600; }
            else if (czl <= 80.9)     { num1 = 1; num2 = 800; }
            else if (czl <= 90.9)     { num1 = 2; num2 = 1200; }
            else                       { num1 = 2; num2 = 2200; }
        } else if (category == 2) {
            if (czl <= 10.9)          { num1 = 1; num2 = 190; }
            else if (czl <= 30.9)     { num1 = 1; num2 = 240; }
            else if (czl <= 49.9)     { num1 = 1; num2 = 340; }
            else if (czl <= 60.9)     { num1 = 1; num2 = 470; }
            else if (czl <= 70.9)     { num1 = 1; num2 = 590; }
            else if (czl <= 80.9)     { num1 = 1; num2 = 780; }
            else if (czl <= 90.9)     { num1 = 2; num2 = 1100; }
            else                       { num1 = 2; num2 = 1800; }
        } else { // category == 3
            if (czl <= 10.9)          { num1 = 1; num2 = 180; }
            else if (czl <= 30.9)     { num1 = 1; num2 = 230; }
            else if (czl <= 49.9)     { num1 = 1; num2 = 330; }
            else if (czl <= 60.9)     { num1 = 1; num2 = 450; }
            else if (czl <= 70.9)     { num1 = 1; num2 = 570; }
            else if (czl <= 80.9)     { num1 = 1; num2 = 760; }
            else if (czl <= 90.9)     { num1 = 2; num2 = 1000; }
            else                       { num1 = 2; num2 = 1500; }
        }
        return new double[]{num1, num2};
    }

    /** PHP: learn skills from bb template skillist */
    private void learnSkillsFromTemplate(UserPet savedPet, Pet template) {
        String skillist = template.getSkillList();
        if (skillist == null || skillist.isEmpty()) return;

        for (String entry : skillist.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            long skillDefId = Long.parseLong(parts[0].trim());
            int skillLevel = Integer.parseInt(parts[1].trim());

            SkillSys skillDef = skillSysRepo.findById(skillDefId).orElse(null);
            if (skillDef == null || skillDef.getAckvalue() == null || skillDef.getAckvalue().isEmpty()) continue;

            String[] ackArr = skillDef.getAckvalue().split(",");
            String[] plusArr = skillDef.getPlus() != null ? skillDef.getPlus().split(",") : new String[]{"0"};
            String[] uhpArr = skillDef.getUhp() != null ? skillDef.getUhp().split(",") : new String[]{"0"};
            String[] umpArr = skillDef.getUmp() != null ? skillDef.getUmp().split(",") : new String[]{"0"};
            String[] imgArr = skillDef.getImgeft() != null ? skillDef.getImgeft().split(",") : new String[]{"0"};

            Skill newSkill = new Skill();
            newSkill.setPetId(savedPet.getId());
            newSkill.setSkillDefId(skillDefId);
            newSkill.setName(skillDef.getName());
            newSkill.setLevel(skillLevel);
            newSkill.setVary(skillDef.getVary());
            newSkill.setWx(skillDef.getWx());
            newSkill.setValue(ackArr.length > 0 ? ackArr[0] : "0");
            newSkill.setPlus(plusArr.length > 0 ? plusArr[0] : "0");
            newSkill.setImg(imgArr.length > 0 ? imgArr[0] : "0");
            newSkill.setUhp(uhpArr.length > 0 ? Integer.parseInt(uhpArr[0]) : 0);
            newSkill.setUmp(umpArr.length > 0 ? Integer.parseInt(umpArr[0]) : 0);

            skillRepo.save(newSkill);
        }
    }
}
