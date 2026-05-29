package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Equipment special effects engine.
 * Parses props.effect, pluseffect, gem sockets, and set bonuses.
 * Matches PHP sec_common_fnc.php formatMsgEffect() + getzbAttrib().
 */
@Service
public class EquipEffectService {
    private static final Logger log = LoggerFactory.getLogger(EquipEffectService.class);
    private static final int DXSH_CAP = 70;

    // 套装阶段配置: seriesName -> effectIndex -> {pieceCount -> multiplier}
    private static final Map<String, Map<Integer, Map<Integer, Double>>> SET_BONUS_CONFIG = new HashMap<>();
    static {
        // 盛世辉煌套装
        Map<Integer, Map<Integer, Double>> sshh = new HashMap<>();
        sshh.put(1, Map.of(6, 0.5, 8, 1.0, 10, 1.5));
        SET_BONUS_CONFIG.put("盛世辉煌套装", sshh);

        // 情殇
        Map<Integer, Map<Integer, Double>> qs = new HashMap<>();
        qs.put(1, Map.of(6, 0.3, 8, 0.6, 9, 0.8));
        qs.put(2, Map.of(6, 0.25, 8, 0.35, 9, 0.55));
        SET_BONUS_CONFIG.put("情殇", qs);

        // 厄菲斯套装
        Map<Integer, Map<Integer, Double>> efs = new HashMap<>();
        efs.put(1, Map.of(6, 0.3, 8, 0.6, 10, 0.8));
        efs.put(2, Map.of(6, 0.25, 8, 0.35, 10, 0.55));
        SET_BONUS_CONFIG.put("厄菲斯套装", efs);

        // 玲珑一套
        Map<Integer, Map<Integer, Double>> ll = new HashMap<>();
        ll.put(1, Map.of(6, 0.15, 8, 0.25, 9, 0.45));
        SET_BONUS_CONFIG.put("玲珑一套", ll);

        // 圣光套装
        Map<Integer, Map<Integer, Double>> sg = new HashMap<>();
        sg.put(1, Map.of(6, 0.15, 8, 0.25, 10, 0.45));
        SET_BONUS_CONFIG.put("圣光套装", sg);

        // 神恩
        Map<Integer, Map<Integer, Double>> se = new HashMap<>();
        se.put(1, Map.of(6, 0.15, 8, 0.25, 9, 0.45));
        SET_BONUS_CONFIG.put("神恩", se);

        // 阿尔提套装
        Map<Integer, Map<Integer, Double>> aet = new HashMap<>();
        aet.put(1, Map.of(6, 0.15, 8, 0.25, 9, 0.45));
        SET_BONUS_CONFIG.put("阿尔提套装", aet);

        // 神圣战场套装
        Map<Integer, Map<Integer, Double>> sszc = new HashMap<>();
        sszc.put(1, Map.of(6, 0.15, 8, 0.25, 9, 0.45));
        SET_BONUS_CONFIG.put("神圣战场套装", sszc);
    }

    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;

    public EquipEffectService(UserBagRepository bagRepo, PropsRepository propsRepo) {
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
    }

    /** All parsed effects for a pet's equipped items */
    public static class Effects {
        // Flat stats
        public long ac, mc, hp, mp, speed, hits, miss;
        // Percent stats (raw percentages, applied in getzbAttrib)
        public int acRate, mcRate, hpRate, mpRate, speedRate, hitsRate, missRate;
        // Battle special effects (raw percentages)
        public int hitshp, hitsmp, dxsh, shjs, sdmp, szmp;
        public int crit;
        public long addMoney;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ac", ac); m.put("mc", mc); m.put("hp", hp); m.put("mp", mp);
            m.put("speed", speed); m.put("hits", hits); m.put("miss", miss);
            m.put("acRate", acRate); m.put("mcRate", mcRate);
            m.put("hpRate", hpRate); m.put("mpRate", mpRate);
            m.put("hitshp", hitshp); m.put("hitsmp", hitsmp);
            m.put("dxsh", dxsh); m.put("shjs", shjs);
            m.put("crit", crit);
            return m;
        }
    }

    /**
     * Parse ALL equipment effects for a pet.
     * Layer 1: base effect + enhancement bonus
     * Layer 2: pluseffect
     * Layer 3: gem sockets (F_item_hole_info)
     * Layer 4: set bonuses (serieseffect)
     */
    public Effects parseAllEffects(Long petId) {
        Effects eff = new Effects();

        List<UserBag> equipped = bagRepo.findByEquipPetId(petId);
        if (equipped.isEmpty()) return eff;

        // Track processed sets
        Set<String> processedSeries = new HashSet<>();

        for (UserBag item : equipped) {
            if (item.getPropId() == null) continue;
            Props props = propsRepo.findById(item.getPropId()).orElse(null);
            if (props == null) continue;

            // Layer 1: base effect + enhancement
            parseBaseEffect(eff, props, item);

            // Layer 2: pluseffect
            if (props.getPluseffect() != null && !props.getPluseffect().isEmpty()) {
                parsePlusEffect(eff, props.getPluseffect());
            }

            // Layer 3: gem sockets
            if (item.getHoleInfo() != null && !item.getHoleInfo().isEmpty()) {
                parseGemEffects(eff, item.getHoleInfo());
            }

            // Layer 4: set bonuses (process each set once)
            if (props.getSeries() != null && !props.getSeries().isEmpty()) {
                String seriesName = extractSeriesName(props.getSeries());
                if (seriesName != null && !processedSeries.contains(seriesName)) {
                    processedSeries.add(seriesName);
                    parseSetBonus(eff, equipped, props.getSeries(), props.getSerieseffect(), petId);
                }
            }
        }

        // Cap dxsh at 70%
        if (eff.dxsh > DXSH_CAP) eff.dxsh = DXSH_CAP;

        return eff;
    }

    private void parseBaseEffect(Effects eff, Props props, UserBag item) {
        if (props.getEffect() == null || props.getEffect().isEmpty()) return;
        for (String pair : props.getEffect().split(",")) {
            String[] kv = pair.split(":");
            if (kv.length < 2) continue;
            long val = parseLong(kv[1]);
            // Enhancement bonus
            if (item.getPlusTimesEffect() != null && !item.getPlusTimesEffect().isEmpty()) {
                String[] tms = item.getPlusTimesEffect().split(",");
                if (tms.length >= 2) val += parseLong(tms[1]);
            }
            addFlat(eff, kv[0], val);
        }
    }

    private void parsePlusEffect(Effects eff, String pluseffect) {
        for (String pair : pluseffect.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length < 2) continue;
            String key = kv[0];
            long val = parseLong(kv[1]);
            switch (key) {
                case "ac" -> eff.ac += val;
                case "mc" -> eff.mc += val;
                case "hp" -> eff.hp += val;
                case "mp" -> eff.mp += val;
                case "speed" -> eff.speed += val;
                case "hits" -> eff.hits += val;
                case "miss" -> eff.miss += val;
                case "hprate" -> eff.hpRate += (int)val;
                case "mprate" -> eff.mpRate += (int)val;
                case "acrate" -> eff.acRate += (int)val;
                case "mcrate" -> eff.mcRate += (int)val;
                case "hitsrate" -> eff.hitsRate += (int)val;
                case "missrate" -> eff.missRate += (int)val;
                case "speedrate" -> eff.speedRate += (int)val;
                case "hitshp" -> eff.hitshp += (int)val;
                case "hitsmp" -> eff.hitsmp += (int)val;
                case "dxsh" -> eff.dxsh += (int)val;
                case "shjs" -> eff.shjs += (int)val;
                case "sdmp" -> eff.sdmp += (int)val;
                case "szmp" -> eff.szmp += (int)val;
                case "crit" -> eff.crit += (int)val;
                case "addmoney" -> eff.addMoney += val;
            }
        }
    }

    /** Gem sockets: flat stats become rate (percentage) */
    private void parseGemEffects(Effects eff, String holeInfo) {
        for (String pair : holeInfo.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length < 2) continue;
            String key = kv[0];
            // Strip trailing %
            String valStr = kv[1].replace("%", "");
            int val = parseInt(valStr);
            switch (key) {
                case "ac" -> eff.acRate += val;
                case "mc" -> eff.mcRate += val;
                case "hp" -> eff.hpRate += val;
                case "mp" -> eff.mpRate += val;
                case "speed" -> eff.speedRate += val;
                case "hits" -> eff.hitsRate += val;
                case "miss" -> eff.missRate += val;
                case "hitshp" -> eff.hitshp += val;
                case "hitsmp" -> eff.hitsmp += val;
                case "dxsh" -> eff.dxsh += val;
                case "shjs" -> eff.shjs += val;
                case "sdmp" -> eff.sdmp += val;
                case "szmp" -> eff.szmp += val;
                case "crit" -> eff.crit += val;
            }
        }
    }

    private void parseSetBonus(Effects eff, List<UserBag> equipped, String series, String seriesEffect, Long petId) {
        if (seriesEffect == null || seriesEffect.isEmpty()) return;
        // Count how many set pieces are equipped
        String[] setPieceIds = extractSeriesPieceIds(series);
        int count = 0;
        for (UserBag item : equipped) {
            if (item.getPropId() != null && containsPiece(setPieceIds, item.getPropId())) {
                count++;
            }
        }
        if (count == 0) return;

        // Get multiplier from config
        String seriesName = extractSeriesName(series);
        Map<Integer, Map<Integer, Double>> bonusConfig = seriesName != null ? SET_BONUS_CONFIG.get(seriesName) : null;

        // Apply effects with multiplier
        String[] effects = seriesEffect.split(",");
        for (int i = 0; i < effects.length; i++) {
            String[] kv = effects[i].split(":");
            if (kv.length < 2) continue;
            String key = kv[0];
            long val = parseLong(kv[1]);

            // Apply multiplier if configured
            double multiplier = 1.0;
            if (bonusConfig != null) {
                Map<Integer, Double> thresholds = bonusConfig.get(i + 1); // effectIndex is 1-based
                if (thresholds != null) {
                    // Find the highest threshold that count meets
                    for (Map.Entry<Integer, Double> entry : thresholds.entrySet()) {
                        if (count >= entry.getKey()) {
                            multiplier = entry.getValue();
                        }
                    }
                }
            }

            val = Math.round(val * multiplier);

            // Same processing as pluseffect
            switch (key) {
                case "ac" -> eff.ac += val;
                case "mc" -> eff.mc += val;
                case "hp" -> eff.hp += val;
                case "mp" -> eff.mp += val;
                case "speed" -> eff.speed += val;
                case "hits" -> eff.hits += val;
                case "miss" -> eff.miss += val;
                case "hprate" -> eff.hpRate += (int)val;
                case "mprate" -> eff.mpRate += (int)val;
                case "acrate" -> eff.acRate += (int)val;
                case "mcrate" -> eff.mcRate += (int)val;
                case "hitsrate" -> eff.hitsRate += (int)val;
                case "missrate" -> eff.missRate += (int)val;
                case "speedrate" -> eff.speedRate += (int)val;
                case "hitshp" -> eff.hitshp += (int)val;
                case "hitsmp" -> eff.hitsmp += (int)val;
                case "dxsh" -> eff.dxsh += (int)val;
                case "shjs" -> eff.shjs += (int)val;
                case "crit" -> eff.crit += (int)val;
                case "addmoney" -> eff.addMoney += val;
            }
        }
    }

    /**
     * Convert percentage effects to flat values using pet base stats.
     * Matches PHP getzbAttrib() percentage-to-value conversion.
     */
    public Effects resolvePercentages(Effects eff, UserPet pet) {
        if (pet.getSrchp() != null) eff.hp += Math.round(eff.hpRate * pet.getSrchp() * 0.01);
        if (pet.getSrcmp() != null) eff.mp += Math.round(eff.mpRate * pet.getSrcmp() * 0.01);
        if (pet.getAc() != null) eff.ac += Math.round(eff.acRate * pet.getAc() * 0.01);
        if (pet.getMc() != null) eff.mc += Math.round(eff.mcRate * pet.getMc() * 0.01);
        if (pet.getHits() != null) eff.hits += Math.round(eff.hitsRate * pet.getHits() * 0.01);
        if (pet.getMiss() != null) eff.miss += Math.round(eff.missRate * pet.getMiss() * 0.01);
        if (pet.getSpeed() != null) eff.speed += Math.round(eff.speedRate * pet.getSpeed() * 0.01);
        return eff;
    }

    /**
     * Compute damage-dependent battle effects.
     * Must be called during battle with actual damage values.
     * @param eff the parsed effects
     * @param petDamage the pet's outgoing damage (for hitshp/hitsmp/shjs)
     * @param monsterDamage the monster's incoming damage (for dxsh/szmp)
     */
    public BattleEffects computeBattleEffects(Effects eff, long petDamage, long monsterDamage) {
        BattleEffects be = new BattleEffects();
        be.lifesteal = Math.round(eff.hitshp * petDamage * 0.01);
        be.manasteal = Math.round(eff.hitsmp * petDamage * 0.01);
        be.damageReduce = Math.round(eff.dxsh * monsterDamage * 0.01);
        be.damageDeepen = Math.round(eff.shjs * petDamage * 0.01);
        be.critRate = eff.crit;
        return be;
    }

    public static class BattleEffects {
        public long lifesteal;    // HP recovered from hitshp
        public long manasteal;    // MP recovered from hitsmp
        public long damageReduce; // Damage subtracted from monster attack (hpdx)
        public long damageDeepen; // Bonus damage added to pet attack (ack)
        public int critRate;      // Additional crit chance

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lifesteal", lifesteal);
            m.put("manasteal", manasteal);
            m.put("damageReduce", damageReduce);
            m.put("damageDeepen", damageDeepen);
            m.put("critRate", critRate);
            return m;
        }
    }

    // ---- Helpers ----

    private void addFlat(Effects eff, String key, long val) {
        switch (key) {
            case "ac" -> eff.ac += val; case "mc" -> eff.mc += val;
            case "hp" -> eff.hp += val; case "mp" -> eff.mp += val;
            case "speed" -> eff.speed += val; case "hits" -> eff.hits += val;
            case "miss" -> eff.miss += val;
        }
    }

    private String extractSeriesName(String series) {
        if (series == null) return null;
        int idx = series.indexOf(":");
        return idx > 0 ? series.substring(0, idx) : series;
    }

    private String[] extractSeriesPieceIds(String series) {
        int idx = series.indexOf(":");
        if (idx <= 0) return new String[0];
        return series.substring(idx + 1).split("\\|");
    }

    private boolean containsPiece(String[] ids, Long propId) {
        for (String id : ids) {
            if (id.equals(String.valueOf(propId))) return true;
        }
        return false;
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s.trim().replace("\n","").replace("\r","")); }
        catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim().replace("\n","").replace("\r","")); }
        catch (NumberFormatException e) { return 0; }
    }
}
