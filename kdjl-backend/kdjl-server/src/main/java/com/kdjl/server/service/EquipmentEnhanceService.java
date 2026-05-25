package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

/**
 * Equipment enhancement system ported from PHP:
 *   zbstrengthGate.php + ext_zbstrength.php (strengthen +1..+15)
 *   zbfjGate.php (decompose)
 *   zbxlGate.php (clear gem holes / wash)
 */
@Service
public class EquipmentEnhanceService {
    private static final Logger log = LoggerFactory.getLogger(EquipmentEnhanceService.class);

    // harden[level] = "successRate_out_of_10,goldCost"
    private static final String[] HARDEN = {
        "6,100", "6,300", "6,600", "5,1000", "5,1500",
        "5,2000", "4,3000", "4,3500", "4,5000", "3,7000",
        "3,10000", "3,15000", "2,20000", "2,30000", "1,50000"
    };
    private static final int MAX_STRENGTHEN = 15;
    private static final int STRENGTHEN_COOLDOWN_SEC = 5;
    private static final int MAX_DECOMPOSE_PER_DAY = 5;
    private static final String COOLDOWN_KEY = "zbstrength:cooldown:";
    private static final String DECOMPOSE_KEY = "zbfj:daily:";

    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;
    private final WelcomeRepository welcomeRepo;
    private final CacheService cache;

    public EquipmentEnhanceService(UserBagRepository bagRepo, PropsRepository propsRepo,
                                   PlayerRepository playerRepo, WelcomeRepository welcomeRepo,
                                   CacheService cache) {
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.playerRepo = playerRepo;
        this.welcomeRepo = welcomeRepo;
        this.cache = cache;
    }

    // ==================== Strengthen ====================

    @Transactional
    public Map<String, Object> strengthen(Long playerId, Long bagItemId, Long auxiliaryBagId) {
        // Cooldown check
        String cdKey = COOLDOWN_KEY + playerId;
        if (cache.exists(cdKey)) {
            throw new IllegalStateException("操作太频繁，请稍后再试");
        }
        cache.set(cdKey, "1", Duration.ofSeconds(STRENGTHEN_COOLDOWN_SEC));

        UserBag equipBag = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("装备不存在"));
        if (!playerId.equals(equipBag.getPlayerId()))
            throw new IllegalArgumentException("不是你的装备");
        if (equipBag.getZbing() != null && equipBag.getZbing() == 1)
            throw new IllegalArgumentException("请先卸下装备再强化");

        Props equipProps = propsRepo.findById(equipBag.getPropId())
            .orElseThrow(() -> new IllegalArgumentException("道具定义不存在"));
        if (equipProps.getVaryname() == null || equipProps.getVaryname() != 9)
            throw new IllegalArgumentException("该物品不是装备");
        if (equipProps.getPlusflag() == null || equipProps.getPlusflag() != 1)
            throw new IllegalArgumentException("该装备不可强化");

        // Current strengthen level from plus_tms_eft (format: "level,bonus")
        int curLevel = 0;
        if (equipBag.getPlusTimesEffect() != null && !equipBag.getPlusTimesEffect().isEmpty()) {
            String[] parts = equipBag.getPlusTimesEffect().split(",");
            curLevel = Integer.parseInt(parts[0]);
        }
        if (curLevel >= MAX_STRENGTHEN) {
            throw new IllegalArgumentException("已达最大强化等级");
        }

        // Material check
        Long materialPropId = equipProps.getPlusPropId();
        int materialNeeded = equipProps.getPlusnum() != null && equipProps.getPlusnum() > 0
            ? equipProps.getPlusnum() : 1;
        UserBag material = findMaterial(playerId, materialPropId, materialNeeded);

        // Gold cost from harden array
        String[] hardenEntry = HARDEN[curLevel].split(",");
        int successRate = Integer.parseInt(hardenEntry[0]); // out of 10
        int goldCost = Integer.parseInt(hardenEntry[1]);

        // Auxiliary item processing
        boolean baodi = false;  // 保底: downgrade on failure instead of destroy
        boolean baodeng = false; // 保等: keep level on failure
        if (auxiliaryBagId != null && auxiliaryBagId > 0) {
            UserBag auxBag = bagRepo.findById(auxiliaryBagId)
                .orElseThrow(() -> new IllegalArgumentException("辅助道具不存在"));
            if (!playerId.equals(auxBag.getPlayerId()))
                throw new IllegalArgumentException("不是你的辅助道具");
            Props auxProps = propsRepo.findById(auxBag.getPropId()).orElse(null);
            if (auxProps == null || auxProps.getEffect() == null)
                throw new IllegalArgumentException("辅助道具无效");

            String[] effParts = auxProps.getEffect().split(":");
            switch (effParts[0]) {
                case "suc" -> {
                    successRate += Integer.parseInt(effParts[1]);
                }
                case "100suc" -> {
                    String[] args = effParts[1].split(",");
                    int threshold = Integer.parseInt(args[1]);
                    if (curLevel < threshold) successRate = 10;
                }
                case "baodi" -> baodi = true;
                case "baodeng" -> baodeng = true;
            }
        }

        // Consume auxiliary item first (consumed regardless of result)
        if (auxiliaryBagId != null && auxiliaryBagId > 0) {
            consumeItem(auxiliaryBagId, 1);
        }

        // Random roll: 1-10, success if <= successRate
        int roll = new Random().nextInt(10) + 1;
        boolean success = roll <= successRate;

        if (success) {
            deductGold(playerId, goldCost);
            consumeItem(material.getId(), 1);
            int newLevel = curLevel + 1;
            String plusget = equipProps.getPlusget();
            String bonus = "0";
            if (plusget != null && !plusget.isEmpty()) {
                String[] bonuses = plusget.split(",");
                if (curLevel < bonuses.length) bonus = bonuses[curLevel];
            }
            equipBag.setPlusTimesEffect(newLevel + "," + bonus);
            bagRepo.save(equipBag);

            log.info("Strengthen SUCCESS: player={}, bagItem={}, level={}->{}, roll={}, rate={}",
                playerId, bagItemId, curLevel, newLevel, roll, successRate);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("newLevel", newLevel);
            result.put("bonus", bonus);
            result.put("goldCost", goldCost);
            return result;
        }

        // Failure handling
        log.info("Strengthen FAILED: player={}, bagItem={}, level={}, roll={}, rate={}",
            playerId, bagItemId, curLevel, roll, successRate);

        if (baodi) {
            int newLevel = Math.max(0, curLevel - 2);
            String plusget = equipProps.getPlusget();
            String bonus = "0";
            if (newLevel > 0 && plusget != null && !plusget.isEmpty()) {
                String[] bonuses = plusget.split(",");
                if (newLevel - 1 < bonuses.length) bonus = bonuses[newLevel - 1];
            }
            if (newLevel == 0) {
                equipBag.setPlusTimesEffect(null);
            } else {
                equipBag.setPlusTimesEffect(newLevel + "," + bonus);
            }
            bagRepo.save(equipBag);
            consumeItem(material.getId(), 1);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("baodi", true);
            result.put("newLevel", newLevel);
            result.put("message", "强化失败，保底触发，降至+" + newLevel);
            return result;
        }

        if (baodeng) {
            consumeItem(material.getId(), 1);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("baodeng", true);
            result.put("message", "强化失败，保等触发，等级不变");
            return result;
        }

        // Normal failure: destroy equipment
        consumeItem(material.getId(), 1);
        bagRepo.delete(equipBag);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("destroyed", true);
        result.put("message", "强化失败，装备已破碎");
        return result;
    }

    // ==================== Decompose ====================

    @Transactional
    public Map<String, Object> decompose(Long playerId, Long bagItemId) {
        String dailyKey = DECOMPOSE_KEY + playerId;
        int used = cache.get(dailyKey, Integer.class).orElse(0);
        if (used >= MAX_DECOMPOSE_PER_DAY) {
            throw new IllegalStateException("今日分解次数已用完");
        }

        UserBag equipBag = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("装备不存在"));
        if (!playerId.equals(equipBag.getPlayerId()))
            throw new IllegalArgumentException("不是你的装备");
        if (equipBag.getZbing() != null && equipBag.getZbing() == 1)
            throw new IllegalArgumentException("请先卸下装备再分解");

        Props equipProps = propsRepo.findById(equipBag.getPropId())
            .orElseThrow(() -> new IllegalArgumentException("道具定义不存在"));
        if (equipProps.getVaryname() == null || equipProps.getVaryname() != 9)
            throw new IllegalArgumentException("该物品不是装备");

        // Check if this equipment position is decomposable
        String biodegradable = welcomeRepo.findByCode("biodegradable_equipment")
            .map(Welcome::getContents).orElse("");
        if (biodegradable.isEmpty())
            throw new IllegalArgumentException("分解功能未开放");

        Set<String> allowedPositions = new HashSet<>(Arrays.asList(biodegradable.split(",")));
        String position = String.valueOf(equipProps.getPostion() != null ? equipProps.getPostion() : 0);
        if (!allowedPositions.contains(position))
            throw new IllegalArgumentException("该部位的装备不能分解");

        // Get success rate config for this equipment color
        String color = equipProps.getPropscolor();
        String rateCode = "fj_" + (color != null ? color : "1") + "_success_rate";
        String rateConfig = welcomeRepo.findByCode(rateCode)
            .map(Welcome::getContents).orElse(null);

        // Delete the equipment
        bagRepo.delete(equipBag);
        cache.set(dailyKey, used + 1, Duration.ofDays(1));

        Map<String, Object> result = new LinkedHashMap<>();

        if (rateConfig == null || rateConfig.isEmpty()) {
            result.put("success", false);
            result.put("message", "分解失败，装备已销毁");
            log.info("Decompose FAIL (no config): player={}, bagItem={}, color={}",
                playerId, bagItemId, color);
            return result;
        }

        // Parse rate config: "itemId:min-max:probRange,itemId:min-max:probRange,..."
        int roll = new Random().nextInt(100) + 1;
        String matchedItem = null;
        int minCount = 0, maxCount = 0;

        for (String entry : rateConfig.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length < 3) continue;
            String itemId = parts[0];
            String[] countRange = parts[1].split("-");
            String[] probRange = parts[2].split("-");
            int probLow = Integer.parseInt(probRange[0]);
            int probHigh = Integer.parseInt(probRange[1]);
            if (roll >= probLow && roll <= probHigh) {
                matchedItem = itemId;
                minCount = Integer.parseInt(countRange[0]);
                maxCount = Integer.parseInt(countRange[1]);
                break;
            }
        }

        if (matchedItem == null) {
            result.put("success", false);
            result.put("message", "分解失败，装备已销毁");
            log.info("Decompose FAIL (bad roll): player={}, bagItem={}, roll={}",
                playerId, bagItemId, roll);
            return result;
        }

        // Success: give items
        int count = minCount == maxCount ? minCount
            : new Random().nextInt(maxCount - minCount + 1) + minCount;
        Props rewardProps = propsRepo.findById(Long.parseLong(matchedItem)).orElse(null);

        addItemToPlayer(playerId, Long.parseLong(matchedItem), count);

        log.info("Decompose SUCCESS: player={}, bagItem={}, reward={}x{}, roll={}",
            playerId, bagItemId, matchedItem, count, roll);

        result.put("success", true);
        result.put("itemName", rewardProps != null ? rewardProps.getName() : "未知物品");
        result.put("count", count);
        result.put("itemId", matchedItem);
        return result;
    }

    // ==================== Clear Gem Holes ====================

    @Transactional
    public Map<String, Object> clearHoles(Long playerId, Long equipmentBagId, Long cleaningItemBagId) {
        if (cleaningItemBagId == null || cleaningItemBagId <= 0)
            throw new IllegalArgumentException("请选择洗炼石");

        UserBag equipBag = bagRepo.findById(equipmentBagId)
            .orElseThrow(() -> new IllegalArgumentException("装备不存在"));
        if (!playerId.equals(equipBag.getPlayerId()))
            throw new IllegalArgumentException("不是你的装备");
        if (equipBag.getHoleInfo() == null || equipBag.getHoleInfo().isEmpty())
            throw new IllegalArgumentException("该装备没有镶嵌宝石，无需洗炼");

        Props equipProps = propsRepo.findById(equipBag.getPropId()).orElse(null);
        if (equipProps == null || equipProps.getVaryname() == null || equipProps.getVaryname() != 9)
            throw new IllegalArgumentException("该物品不是装备");

        UserBag cleaningBag = bagRepo.findById(cleaningItemBagId)
            .orElseThrow(() -> new IllegalArgumentException("洗炼道具不存在"));
        if (!playerId.equals(cleaningBag.getPlayerId()))
            throw new IllegalArgumentException("不是你的洗炼道具");

        Props cleaningProps = propsRepo.findById(cleaningBag.getPropId()).orElse(null);
        if (cleaningProps == null || cleaningProps.getVaryname() == null
            || cleaningProps.getVaryname() != 26)
            throw new IllegalArgumentException("该物品不是洗炼道具");
        if (cleaningProps.getEffect() == null || !cleaningProps.getEffect().equals("clear"))
            throw new IllegalArgumentException("洗炼道具无效");

        equipBag.setHoleInfo(null);
        bagRepo.save(equipBag);

        consumeItem(cleaningItemBagId, 1);

        log.info("ClearHoles: player={}, equipBag={}, consumed cleaning item={}",
            playerId, equipmentBagId, cleaningItemBagId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "宝石孔已清除");
        return result;
    }

    // ---- Helpers ----

    private UserBag findMaterial(Long playerId, Long propId, int needed) {
        List<UserBag> bags = bagRepo.findByPlayerId(playerId);
        for (UserBag b : bags) {
            if (b.getPropId() != null && b.getPropId().equals(propId)
                && (b.getSums() != null && b.getSums() >= needed)) {
                return b;
            }
        }
        throw new IllegalArgumentException("强化材料不足");
    }

    private void consumeItem(Long bagId, int count) {
        UserBag item = bagRepo.findById(bagId).orElseThrow();
        int current = item.getSums() != null ? item.getSums() : 0;
        if (current < count) throw new IllegalArgumentException("物品数量不足");
        if (current == count) {
            bagRepo.delete(item);
        } else {
            item.setSums(current - count);
            bagRepo.save(item);
        }
    }

    private void deductGold(Long playerId, int amount) {
        Player player = playerRepo.findById(playerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        int current = player.getMoney() != null ? player.getMoney() : 0;
        if (current < amount) throw new IllegalArgumentException("金币不足");
        player.setMoney(current - amount);
        playerRepo.save(player);
    }

    private void addItemToPlayer(Long playerId, Long propId, int count) {
        List<UserBag> bags = bagRepo.findByPlayerId(playerId);
        for (UserBag b : bags) {
            if (b.getPropId() != null && b.getPropId().equals(propId)) {
                b.setSums((b.getSums() != null ? b.getSums() : 0) + count);
                bagRepo.save(b);
                return;
            }
        }
        UserBag newBag = new UserBag();
        newBag.setPlayerId(playerId);
        newBag.setPropId(propId);
        newBag.setSums(count);
        newBag.setVary(1);
        newBag.setZbing(0);
        newBag.setPyb(0);
        bagRepo.save(newBag);
    }
}
