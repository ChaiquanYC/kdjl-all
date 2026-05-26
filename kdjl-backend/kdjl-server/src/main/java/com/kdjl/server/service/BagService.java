package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BagService {

    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final UserPetRepository userPetRepo;
    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private final PetRepository petRepo;
    private final SkillSysRepository skillSysRepo;
    private final SkillRepository skillRepo;
    private final WarPlayerRepository warPlayerRepo;
    private final WarFighterRepository warFighterRepo;
    private final WarFighterTalentRepository warTalentRepo;
    private final LevelUpService levelUpService;

    public BagService(UserBagRepository bagRepo, PropsRepository propsRepo,
                      UserPetRepository userPetRepo, PlayerRepository playerRepo,
                      PlayerExtRepository playerExtRepo, PetRepository petRepo,
                      SkillSysRepository skillSysRepo, SkillRepository skillRepo,
                      WarPlayerRepository warPlayerRepo, WarFighterRepository warFighterRepo,
                      WarFighterTalentRepository warTalentRepo, LevelUpService levelUpService) {
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.userPetRepo = userPetRepo;
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.petRepo = petRepo;
        this.skillSysRepo = skillSysRepo;
        this.skillRepo = skillRepo;
        this.warPlayerRepo = warPlayerRepo;
        this.warFighterRepo = warFighterRepo;
        this.warTalentRepo = warTalentRepo;
        this.levelUpService = levelUpService;
    }

    public List<Map<String, Object>> getPlayerBag(Long playerId) {
        List<UserBag> items = bagRepo.findByPlayerId(playerId);
        return items.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("propId", i.getPropId());
            m.put("count", i.getSums());
            boolean isEquip = i.getVary() != null && i.getVary() == 2;
            m.put("vary", isEquip ? "equipment" : "item");
            m.put("equipPetId", i.getEquipPetId());
            m.put("zbing", i.getZbing());
            m.put("sell", i.getSell());
            Props p = propsRepo.findById(i.getPropId() != null ? i.getPropId().longValue() : 0).orElse(null);
            if (p != null) {
                m.put("name", p.getName());
                m.put("img", p.getImg());
                m.put("propsColor", p.getPropscolor());
                m.put("varyname", p.getVaryname());
                m.put("effect", p.getEffect());
                m.put("requires", p.getRequires());
                m.put("buy", p.getBuy());
                m.put("yb", p.getYb());
                m.put("usages", p.getUsages());
                m.put("cantrade", i.getCantrade());
                m.put("propslock", p.getPropslock() != null ? p.getPropslock() : 0);
                m.put("series", p.getSeries());
                m.put("serieseffect", p.getSerieseffect());
                m.put("pluseffect", p.getPluseffect());
                m.put("plusflag", p.getPlusflag());
                m.put("pluspid", p.getPlusPropId());
                m.put("plusget", p.getPlusget());
                m.put("plusnum", p.getPlusnum());
                m.put("prestige", p.getPrestige());
                m.put("postion", p.getPostion());
                String expire;
                if (p.getEndtime() != null && p.getEndtime() > 0) {
                    long expireEnd = (i.getStime() != null ? i.getStime() : 0) + p.getEndtime();
                    expire = expireEnd > System.currentTimeMillis() / 1000
                        ? "到期时间:" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(expireEnd * 1000))
                        : "过期";
                } else {
                    expire = "永久";
                }
                m.put("expire", expire);
            }
            int cat = p != null && p.getVaryname() != null ? p.getVaryname() : 0;
            m.put("category", CATEGORIES.getOrDefault(cat, "其他" + cat));
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getEquipment(Long playerId) {
        return bagRepo.findByPlayerIdAndVary(playerId, 2).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("propId", i.getPropId());
            m.put("equipPetId", i.getEquipPetId());
            m.put("zbing", i.getZbing()); // 1=equipped
            m.put("holeInfo", i.getHoleInfo());
            if (i.getPropId() != null) {
                Props p = propsRepo.findById(i.getPropId().longValue()).orElse(null);
                if (p != null) {
                    m.put("name", p.getName());
                    m.put("img", p.getImg());
                    m.put("effect", p.getEffect());
                }
            }
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Use a consumable item on a pet.
     * Parses props.effect field: hp:X, mp:X, exp:X, openmap:X, etc.
     */
    @Transactional
    public Map<String, Object> useItem(Long playerId, Long bagItemId, Long petId) {
        return useItem(playerId, bagItemId, petId, false);
    }

    public Map<String, Object> useItem(Long playerId, Long bagItemId, Long petId, boolean isJs) {
        UserBag bagItem = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!bagItem.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("不是你的物品");
        }

        Props props = propsRepo.findById(bagItem.getPropId().longValue())
            .orElseThrow(() -> new IllegalArgumentException("道具定义不存在"));

        // PHP usedProps.php:862 — magic stones must be used in divination house
        if (props.getVaryname() != null && props.getVaryname() == 22 && !isJs) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "占卜石，请在占卜屋中占卜时使用！");
            error.put("redirectTo", "zhanbu");
            return error;
        }

        // Equipment (varyname==9): auto-equip to main battle pet
        if (props.getVaryname() != null && props.getVaryname() == 9) {
            Player player = playerRepo.findById(playerId.intValue()).orElse(null);
            if (player == null || player.getMbid() == null)
                throw new IllegalArgumentException("您还没有设置主战宝宝，不能进行装备！");
            return equipItem(playerId, bagItemId, player.getMbid().longValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usedItemId", bagItemId);
        result.put("propName", props.getName());

        // PHP usedProps.php:1026-1038 — sacred pet restriction
        if (petId != null && petId > 0 && props.getRequires() != null) {
            UserPet targetPet = userPetRepo.findById(petId).orElse(null);
            if (targetPet != null && targetPet.getWx() != null) {
                boolean isSacred = targetPet.getWx() == 7;
                boolean requiresSacred = "__SS__".equals(props.getRequires().trim());
                if (isSacred && !requiresSacred) {
                    result.put("error", "神圣宠物无法使用此道具！");
                    return result;
                }
                if (!isSacred && requiresSacred) {
                    result.put("error", "此道具仅限神圣宠物使用！");
                    return result;
                }
            }
        }

        String effect = props.getEffect();
        if (effect == null || effect.isEmpty()) {
            // No effect — just remove item
            decrementOrRemove(bagItem);
            result.put("effect", "none");
            result.put("message", "使用成功");
            return result;
        }

        // Handle bag/depot/muchang upgrade scrolls by item ID (PHP usedProps.php line 475-515)
        Long propId = props.getId();
        if ((propId >= 200 && propId <= 202) || propId == 1344) {
            String name = props.getName();
            Player p = playerRepo.findById(playerId.intValue()).orElse(null);
            if (p != null) {
                if ("仓库升级卷轴".equals(name)) {
                    int cur = p.getMaxBase() != null ? p.getMaxBase() : 50;
                    if (cur >= 96) { result.put("error", "仓库已扩展到极限"); }
                    else { p.setMaxBase(Math.min(cur + 6, 96)); playerRepo.save(p); result.put("type", "depotExpand"); result.put("newMaxBase", p.getMaxBase()); }
                } else if ("背包升级卷轴".equals(name)) {
                    int cur = p.getMaxBag() != null ? p.getMaxBag() : 30;
                    if (cur >= 96) { result.put("error", "背包已扩展到极限"); }
                    else { p.setMaxBag(Math.min(cur + 6, 96)); playerRepo.save(p); result.put("type", "bagExpand"); result.put("newMaxBag", p.getMaxBag()); }
                } else if ("牧场升级卷轴".equals(name)) {
                    int cur = p.getMaxMc() != null ? p.getMaxMc() : 10;
                    if (cur >= 40) { result.put("error", "牧场已扩展到极限"); }
                    else { p.setMaxMc(Math.min(cur + 6, 40)); playerRepo.save(p); result.put("type", "mcExpand"); }
                }
                if (result.containsKey("type")) {
                    result.put("message", "使用道具 " + name + " 成功!");
                    decrementOrRemove(bagItem);
                }
            }
            return result;
        }

        // Handle giveitems/randitem chests (complex format with | and , delimiters)
        if (effect.startsWith("giveitems:") || effect.startsWith("randitem:")) {
            // PHP usedProps.php:730 — check bag has >= 3 free slots
            Player player = playerRepo.findById(playerId.intValue()).orElse(null);
            int maxBag = player != null && player.getMaxBag() != null ? player.getMaxBag() : 30;
            int currentItems = (int) bagRepo.findByPlayerId(playerId).stream()
                .filter(b -> b.getSums() != null && b.getSums() > 0 && (b.getZbing() == null || b.getZbing() == 0))
                .count();
            if (currentItems >= maxBag - 2) {
                result.put("error", "背包空间不足（需要至少3格空位），当前" + currentItems + "/" + maxBag);
                result.put("skipConsume", true);
                return result;
            }

            String chestBody = effect.substring(effect.indexOf(':') + 1);
            boolean isRandom = effect.startsWith("randitem:");
            String[] items = isRandom ? chestBody.split("\\|") : chestBody.split(",");
            List<Map<String, Object>> givenItems = new ArrayList<>();
            boolean shouldAnnounce = false;

            for (String itemStr : items) {
                String[] parts = itemStr.split(":");
                if (parts.length < 2) continue;
                try {
                    long givePropId = Long.parseLong(parts[0].trim());
                    int giveCount = Integer.parseInt(parts[1].trim());
                    if (isRandom) {
                        // Random chest: probability is 1-in-N. parts: propId,count,prob,announceFlag
                        int prob = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 100;
                        if ((int)(Math.random() * prob) >= 1) continue;
                        // PHP usedProps.php:770 — announce flag: 1=announce, 2=no announce
                        int announceFlag = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 2;
                        if (announceFlag == 1) shouldAnnounce = true;
                        addItemToBag(playerId, givePropId, giveCount);
                        givenItems.add(Map.of("propId", givePropId, "count", giveCount));
                        // PHP usedProps.php: recursion — check if given item is also a chest
                        openChestRecursive(playerId, givePropId, giveCount, givenItems);
                        break; // randitem picks only ONE
                    } else {
                        // Fixed chest: all items guaranteed
                        addItemToBag(playerId, givePropId, giveCount);
                        givenItems.add(Map.of("propId", givePropId, "count", giveCount));
                        openChestRecursive(playerId, givePropId, giveCount, givenItems);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (!givenItems.isEmpty()) {
                result.put("type", "chest");
                result.put("items", givenItems);
                result.put("message", "恭喜获得" + givenItems.size() + "种道具！");
                if (shouldAnnounce) result.put("announce", true);
            }
            decrementOrRemove(bagItem);
            return result;
        }

        // Parse effects: "hp:200,mp:100" or "addyb:1,5" (range with comma)
        String[] rawParts = effect.split(",");
        List<String[]> mergedParts = new ArrayList<>();
        for (int i = 0; i < rawParts.length; i++) {
            String p = rawParts[i].trim();
            if (p.contains(":")) {
                mergedParts.add(new String[]{p});
            } else if (!mergedParts.isEmpty()) {
                // Range value — merge with previous effect (e.g. "addyb:1" + "5" → "addyb:1,5")
                String[] prev = mergedParts.get(mergedParts.size() - 1);
                prev[0] = prev[0] + "," + p;
            }
        }
        for (String[] merged : mergedParts) {
            String part = merged[0];
            int colonIdx = part.indexOf(':');
            if (colonIdx < 0) continue;
            String key = part.substring(0, colonIdx).trim();
            String valStr = part.substring(colonIdx + 1).trim();
            // Parse value: may be "200", "1,5" (range), "(5000000,5000000)", or "1.5:3600"
            int value;
            double dblValue = 0; // for decimal values like exp multiplier 1.5
            String valClean = valStr.replace("(", "").replace(")", "");
            // For multi-part values like "1.5:3600" (exp multiplier), take first part
            String firstPart = valClean.split(":")[0].split(",")[0].trim();
            try {
                dblValue = Double.parseDouble(firstPart);
                // For exp multiplier/addczl, scale by 10 (0.1→1). For others keep raw int.
                if ("exp".equals(key) || "addczl".equals(key)) value = (int)(dblValue * 10);
                else value = (int) dblValue;
            } catch (NumberFormatException e) {
                continue;
            }
            // For range effects (addsj/addyb/addexp), compute random value
            if (valClean.contains(",") && valClean.contains(":") == false) {
                String[] range = valClean.split(",");
                try {
                    int min = Integer.parseInt(range[0].trim());
                    int max = Integer.parseInt(range[1].trim());
                    value = min + (int)(Math.random() * (max - min + 1));
                } catch (NumberFormatException ignored) {}
            }
            applyEffectPart(key, value, playerId, petId, bagItem, result);
        }

        if (result.get("skipConsume") == null || !(Boolean) result.get("skipConsume")) {
            decrementOrRemove(bagItem);
        }
        return result;
    }

    private void applyEffectPart(String key, int value, Long playerId, Long petId,
                                  UserBag bagItem, Map<String, Object> result) {
        switch (key) {
            case "hp" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long maxHp = Math.max(1, (pet.getSrchp() != null ? pet.getSrchp() : 100)
                            + (pet.getAddhp() != null ? pet.getAddhp() : 0));
                        long currentHp = pet.getHp() != null ? pet.getHp() : maxHp;
                        long newHp = Math.min(maxHp, currentHp + value);
                        pet.setHp(newHp);
                        userPetRepo.save(pet);
                        result.put("type", "healHP");
                        result.put("healedHP", newHp - currentHp);
                        result.put("petHP", newHp);
                        if (newHp >= maxHp) result.put("message", "HP已恢复至满！");
                    }
                }
            }
            case "mp" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long maxMp = Math.max(1, (pet.getSrcmp() != null ? pet.getSrcmp() : 50)
                            + (pet.getAddmp() != null ? pet.getAddmp() : 0));
                        long currentMp = pet.getMp() != null ? pet.getMp() : maxMp;
                        long newMp = Math.min(maxMp, currentMp + value);
                        pet.setMp(newMp);
                        userPetRepo.save(pet);
                        result.put("type", "healMP");
                        result.put("healedMP", newMp - currentMp);
                        result.put("petMP", newMp);
                        if (newMp >= maxMp) result.put("message", "MP已恢复至满！");
                    }
                }
            }
            case "addexp" -> {
                // Direct EXP grant (经验月饼/卷轴: addexp:MIN,MAX)
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        boolean leveledUp = levelUpService.addExp(pet, value);
                        result.put("type", "addexp");
                        result.put("expGained", value);
                        if (leveledUp) {
                            result.put("levelUp", true);
                            result.put("newLevel", pet.getLevel());
                        }
                        result.put("message", "恭喜获得" + value + "经验！");
                    }
                }
            }
            case "exp" -> {
                // Double EXP multiplier (双倍经验卷轴: exp:1.5:3600)
                // value is scaled: 15=1.5x, 20=2x, 25=2.5x, 30=3x
                Player p = playerRepo.findById(playerId.intValue()).orElse(null);
                if (p != null) {
                    double mult = value / 10.0;
                    // Map to dblexpflag: 1.5→2, 2→3, 2.5→4, 3→5
                    int dblFlag = (int)(mult * 2);
                    if (dblFlag >= 2 && dblFlag <= 5) {
                        p.setDblExpFlag(dblFlag);
                        p.setDblsTime((int)(System.currentTimeMillis() / 1000));
                        p.setMaxDblExpTime(3600);
                        playerRepo.save(p);
                        result.put("type", "doubleExp");
                        result.put("multiplier", mult);
                        result.put("message", "开启" + mult + "倍经验，持续1小时！");
                    }
                }
            }
            case "openmap" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    String currentOpenMap = player.getOpenMap() != null ? player.getOpenMap() : "";
                    String mapIdStr = String.valueOf(value);
                    boolean alreadyOpen = java.util.Arrays.asList(currentOpenMap.split(",")).contains(mapIdStr);
                    if (alreadyOpen) {
                        result.put("type", "openMap");
                        result.put("message", "该钥匙对应的地图已经打开了!");
                        result.put("skipConsume", true);
                    } else {
                        String newOpenMap = currentOpenMap.isEmpty()
                            ? mapIdStr : currentOpenMap + "," + mapIdStr;
                        player.setOpenMap(newOpenMap);
                        playerRepo.save(player);
                        result.put("type", "openMap");
                        result.put("mapId", value);
                        result.put("message", "对应地图打开成功!");
                    }
                }
            }
            case "zhanshi" -> {
                PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
                if (ext != null) {
                    int cur = ext.getPetShow() != null ? ext.getPetShow() : 0;
                    ext.setPetShow(cur + value);
                    playerExtRepo.save(ext);
                    result.put("type", "showChance");
                    result.put("showChances", ext.getPetShow());
                    result.put("message", "恭喜您增加" + value + "次宠物展示机会！");
                }
            }
            case "addsj" -> {
                // Random crystal amount: eff[1] = "min,max"
                PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
                int cur = ext != null && ext.getSj() != null ? ext.getSj() : 0;
                int gained = value; // value is already parsed as single number; for ranges we handle below
                if (ext == null) {
                    ext = new PlayerExt();
                    ext.setPlayerId(playerId.intValue());
                    ext.setSj(gained);
                    ext.setPetShow(5);
                } else {
                    ext.setSj(cur + gained);
                }
                playerExtRepo.save(ext);
                result.put("type", "crystal");
                result.put("crystals", gained);
                result.put("message", "恭喜您得到了" + gained + "个水晶！");
            }
            case "addyb" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    int curYb = player.getYb() != null ? player.getYb() : 0;
                    int newYb = Math.min(curYb + value, Integer.MAX_VALUE);
                    player.setYb(newYb);
                    playerRepo.save(player);
                    result.put("type", "yuanbao");
                    result.put("ybGained", value);
                    result.put("message", "恭喜您得到了" + value + "元宝！");
                }
            }
            case "addbag" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player == null) break;
                int cur = player.getMaxBag() != null ? player.getMaxBag() : 30;
                if (cur < 150) { result.put("error", "背包需要先达到150格才能使用此道具"); break; }
                if (cur >= 200) { result.put("error", "背包已达到200格上限"); break; }
                int newBag = Math.min(cur + value, 200);
                player.setMaxBag(newBag);
                playerRepo.save(player);
                result.put("type", "bagExpand");
                result.put("newMaxBag", newBag);
                result.put("message", "恭喜您背包格子扩充了" + value + "格！");
                result.put("expansion", value);
            }
            case "addbag1" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player == null) break;
                int cur = player.getMaxBag() != null ? player.getMaxBag() : 30;
                if (cur < 200) { result.put("error", "背包需要先达到200格才能使用此道具"); break; }
                if (cur >= 300) { result.put("error", "背包已达到300格上限"); break; }
                int newBag = Math.min(cur + value, 300);
                player.setMaxBag(newBag);
                playerRepo.save(player);
                result.put("type", "bagExpand");
                result.put("newMaxBag", newBag);
                result.put("message", "恭喜您背包格子扩充了" + value + "格！");
                result.put("expansion", value);
            }
            case "addck" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player == null) break;
                int cur = player.getMaxBase() != null ? player.getMaxBase() : 50;
                if (cur < 150) { result.put("error", "仓库需要先达到150格才能使用此道具"); break; }
                if (cur >= 200) { result.put("error", "仓库已达到200格上限"); break; }
                int newCk = Math.min(cur + value, 200);
                player.setMaxBase(newCk);
                playerRepo.save(player);
                result.put("type", "depotExpand");
                result.put("newMaxBase", newCk);
                result.put("message", "恭喜您仓库格子扩充了" + value + "格！");
                result.put("expansion", value);
            }
            case "addck1" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player == null) break;
                int cur = player.getMaxBase() != null ? player.getMaxBase() : 50;
                if (cur < 200) { result.put("error", "仓库需要先达到200格才能使用此道具"); break; }
                if (cur >= 300) { result.put("error", "仓库已达到300格上限"); break; }
                int newCk = Math.min(cur + value, 300);
                player.setMaxBase(newCk);
                playerRepo.save(player);
                result.put("type", "depotExpand");
                result.put("newMaxBase", newCk);
                result.put("message", "恭喜您仓库格子扩充了" + value + "格！");
                result.put("expansion", value);
            }
            case "autofree" -> {
                // Gold auto-fight count increase
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    int cur = player.getSysAutoSum() != null ? player.getSysAutoSum() : 0;
                    player.setSysAutoSum(cur + value);
                    playerRepo.save(player);
                    result.put("type", "autoFree");
                    result.put("autoFreeAdded", value);
                    result.put("message", "恭喜您增加了" + value + "次金币版自动战斗次数！");
                }
            }
            case "auto" -> {
                // YB auto-fight count increase
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    int cur = player.getMaxAutoFitSum() != null ? player.getMaxAutoFitSum() : 0;
                    player.setMaxAutoFitSum(cur + value);
                    playerRepo.save(player);
                    result.put("type", "autoYB");
                    result.put("autoYBAdded", value);
                    result.put("message", "恭喜您增加了" + value + "次元宝版自动战斗次数！");
                }
            }
            case "autoteam" -> {
                PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
                if (ext != null) {
                    int cur = ext.getTeamAutoTimes() != null ? ext.getTeamAutoTimes() : 0;
                    ext.setTeamAutoTimes(cur + value);
                    playerExtRepo.save(ext);
                    result.put("type", "autoTeam");
                    result.put("autoTeamAdded", value);
                    result.put("message", "恭喜您增加了" + value + "次组队自动战斗次数！");
                }
            }
            case "openpet" -> {
                // Open pet egg: create a new pet from bb template (PHP varyname=15)
                Pet template = petRepo.findById((long) value).orElse(null);
                if (template == null) {
                    result.put("error", "宠物模板不存在");
                    result.put("skipConsume", true);
                    break;
                }
                if (playerId == null) break;
                // PHP: check pet carry limit (max 3)
                List<UserPet> currentPets = userPetRepo.findByPlayerId(playerId);
                if (currentPets.size() >= 3) {
                    result.put("error", "宠物已满！目前最多携带3只宠物");
                    result.put("skipConsume", true);
                    break;
                }
                UserPet newPet = new UserPet();
                newPet.setPlayerId(playerId);
                newPet.setName(template.getName());
                newPet.setLevel(1);
                newPet.setWx(template.getWx());
                newPet.setSrchp(template.getHp()); newPet.setHp(template.getHp());
                newPet.setSrcmp(template.getMp()); newPet.setMp(template.getMp());
                newPet.setAc(template.getAc()); newPet.setMc(template.getMc());
                newPet.setHits(template.getHits().longValue()); newPet.setMiss(template.getMiss().longValue());
                newPet.setSpeed(template.getSpeed().longValue());
                newPet.setNowexp(0L); newPet.setLexp(100L);
                newPet.setImgstand(template.getImgstand()); newPet.setImgack(template.getImgack());
                newPet.setImgdie(template.getImgdie()); newPet.setHeadimg(template.getHeadimg());
                newPet.setCardimg(template.getCardimg());
                newPet.setKx(template.getKx()); newPet.setSkillList(template.getSkillList());
                if (template.getCzl() != null && template.getCzl().contains(",")) {
                    String[] czlRange = template.getCzl().replace(".","").split(",");
                    int minCzl = Integer.parseInt(czlRange[0]);
                    int maxCzl = Integer.parseInt(czlRange[1]);
                    double czl = (minCzl + (int)(Math.random() * (maxCzl - minCzl + 1))) / 10.0;
                    newPet.setCzl(String.valueOf(czl));
                } else {
                    newPet.setCzl(template.getCzl() != null ? template.getCzl() : "1.0");
                }
                newPet.setStime(System.currentTimeMillis() / 1000);
                newPet = userPetRepo.save(newPet);
                // Auto-add 普通攻击 skill (PHP behavior)
                SkillSys basicAtk = skillSysRepo.findById(1L).orElse(null);
                if (basicAtk != null) {
                    Skill s = new Skill();
                    s.setPetId(newPet.getId()); s.setSkillDefId(1L); s.setName("普通攻击");
                    s.setLevel(1); s.setWx(0); s.setVary("1"); s.setValue("0"); s.setPlus("");
                    s.setUhp(0); s.setUmp(0); s.setImg("0");
                    skillRepo.save(s);
                    String currentSkills = newPet.getSkillList();
                    newPet.setSkillList((currentSkills != null && !currentSkills.isEmpty())
                        ? currentSkills + ",1:1" : "1:1");
                    userPetRepo.save(newPet);
                }
                result.put("type", "openPet");
                result.put("petId", newPet.getId());
                result.put("petName", newPet.getName());
                result.put("message", "恭喜您获得宠物：" + newPet.getName() + "！");
            }
            case "addczl" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        String curCzl = pet.getCzl() != null ? pet.getCzl() : "1.0";
                        try {
                            double cur = Double.parseDouble(curCzl);
                            pet.setCzl(String.valueOf(cur + value / 10.0));
                            userPetRepo.save(pet);
                            result.put("type", "gainCzl");
                            result.put("czlAdded", value / 10.0);
                            result.put("message", "恭喜宠物成长率增加了" + (value / 10.0) + "！");
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            case "addac" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long cur = pet.getAc() != null ? pet.getAc() : 0;
                        pet.setAc(cur + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainAc");
                        result.put("acAdded", value);
                        result.put("message", "恭喜宠物攻击力永久增加了" + value + "点！");
                    }
                }
            }
            case "addmc" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long cur = pet.getMc() != null ? pet.getMc() : 0;
                        pet.setMc(cur + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainMc");
                        result.put("mcAdded", value);
                        result.put("message", "恭喜宠物防御力永久增加了" + value + "点！");
                    }
                }
            }
            case "addhp" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        pet.setSrchp((pet.getSrchp() != null ? pet.getSrchp() : 0) + value);
                        pet.setAddhp((pet.getAddhp() != null ? pet.getAddhp() : 0) + value);
                        pet.setHp((pet.getHp() != null ? pet.getHp() : 0) + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainHp");
                        result.put("hpAdded", value);
                        result.put("message", "恭喜宠物生命上限永久增加了" + value + "点！");
                    }
                }
            }
            case "addmp" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        pet.setSrcmp((pet.getSrcmp() != null ? pet.getSrcmp() : 0) + value);
                        pet.setAddmp((pet.getAddmp() != null ? pet.getAddmp() : 0) + value);
                        pet.setMp((pet.getMp() != null ? pet.getMp() : 0) + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainMp");
                        result.put("mpAdded", value);
                        result.put("message", "恭喜宠物魔法上限永久增加了" + value + "点！");
                    }
                }
            }
            case "addspeed" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long cur = pet.getSpeed() != null ? pet.getSpeed() : 0;
                        pet.setSpeed(cur + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainSpeed");
                        result.put("speedAdded", value);
                        result.put("message", "恭喜宠物速度永久增加了" + value + "点！");
                    }
                }
            }
            case "addhits" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long cur = pet.getHits() != null ? pet.getHits() : 0;
                        pet.setHits(cur + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainHits");
                        result.put("hitsAdded", value);
                        result.put("message", "恭喜宠物命中率永久增加了" + value + "点！");
                    }
                }
            }
            case "addmiss" -> {
                if (petId != null && petId > 0) {
                    UserPet pet = userPetRepo.findById(petId).orElse(null);
                    if (pet != null) {
                        long cur = pet.getMiss() != null ? pet.getMiss() : 0;
                        pet.setMiss(cur + value);
                        userPetRepo.save(pet);
                        result.put("type", "gainMiss");
                        result.put("missAdded", value);
                        result.put("message", "恭喜宠物闪避率永久增加了" + value + "点！");
                    }
                }
            }
            case "weiwang" -> {
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    int cur = player.getPrestige() != null ? player.getPrestige() : 0;
                    player.setPrestige(cur + value);
                    playerRepo.save(player);
                    result.put("type", "prestige");
                    result.put("prestigeAdded", value);
                    result.put("message", "恭喜您增加了" + value + "点威望！");
                }
            }
            case "add_cq_czl" -> {
                PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
                if (ext != null) {
                    int cur = ext.getCzlSs() != null ? ext.getCzlSs() : 0;
                    ext.setCzlSs(cur + value);
                    playerExtRepo.save(ext);
                    result.put("type", "czlSs");
                    result.put("czlSsAdded", value);
                    result.put("message", "恭喜您增加了" + value + "点抽取成长值！");
                }
            }
            case "add_zc_jifen" -> {
                PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
                if (ext != null) {
                    // PHP: battlefield points stored in player_ext.buff_status
                    result.put("type", "battleScore");
                    result.put("battleScoreAdded", value);
                    result.put("message", "恭喜您增加了" + value + "点战场积分！");
                }
            }
            case "tuoguan" -> {
                // PHP usedProps.php:426 — increase nursery time
                Player player = playerRepo.findById(playerId.intValue()).orElse(null);
                if (player != null) {
                    int cur = player.getTgTime() != null ? player.getTgTime() : 0;
                    player.setTgTime(cur + value * 3600); // hours → seconds
                    playerRepo.save(player);
                    result.put("type", "tuoguan");
                    result.put("hours", value);
                    result.put("message", "恭喜您增加了" + value + "小时托管时间！");
                }
            }
            // Note: addmc (ranch expansion) is handled above by propId matching (牧场升级卷轴)
            case "tianfuexp" -> {
                // PHP usedProps.php:1902 — talent exp distributed among fighters
                if (petId != null && petId > 0) {
                    List<WarFighter> fighters = warFighterRepo.findByUserId(playerId);
                    if (fighters.isEmpty()) {
                        result.put("type", "talentExp");
                        result.put("message", "恭喜获得天赋经验，但暂无魔塔战士可以分配！");
                    } else {
                        int totalTalents = 0;
                        for (WarFighter f : fighters) {
                            List<WarFighterTalent> talents = warTalentRepo.findByFighterId(f.getFighterId());
                            if (!talents.isEmpty()) {
                                int expPerTalent = value / (fighters.size() * talents.size());
                                for (WarFighterTalent t : talents) {
                                    int cur = t.getCurrentExperience() != null ? t.getCurrentExperience() : 0;
                                    t.setCurrentExperience(cur + expPerTalent);
                                    warTalentRepo.save(t);
                                    totalTalents++;
                                }
                            }
                        }
                        result.put("type", "talentExp");
                        result.put("expGiven", value);
                        result.put("talents", totalTalents);
                        result.put("message", "天赋经验已分配！" + value + "点经验分给" + totalTalents + "个天赋");
                    }
                }
            }
            case "xiedaibb20" -> {
                // PHP usedProps.php:1812 — permanent +2 pet slots
                WarPlayer wp = warPlayerRepo.findById(playerId).orElse(null);
                if (wp != null) {
                    int cur = wp.getMaxTakePetNum() != null ? wp.getMaxTakePetNum() : 1;
                    wp.setMaxTakePetNum(Math.min(cur + 2, 5));
                    warPlayerRepo.save(wp);
                    result.put("type", "petSlot");
                    result.put("slots", wp.getMaxTakePetNum());
                    result.put("message", "宠物栏位永久+2！当前可携带：" + wp.getMaxTakePetNum() + "只");
                } else {
                    result.put("message", "魔塔系统尚未激活，道具已消耗");
                }
            }
            case "xiedaibb21" -> {
                // PHP: 30 days +2 pet slots
                WarPlayer wp = warPlayerRepo.findById(playerId).orElse(null);
                if (wp != null) {
                    int cur = wp.getMaxTakePetNum() != null ? wp.getMaxTakePetNum() : 1;
                    int limit = wp.getTakePetLimitTime() != null ? wp.getTakePetLimitTime() : 0;
                    wp.setMaxTakePetNum(Math.min(cur + 2, 5));
                    wp.setTakePetLimitTime(limit + 30 * 86400); // 30 days in seconds
                    warPlayerRepo.save(wp);
                    result.put("type", "petSlot");
                    result.put("slots", wp.getMaxTakePetNum());
                    result.put("message", "宠物栏位+2（30天）！当前可携带：" + wp.getMaxTakePetNum() + "只");
                } else {
                    result.put("message", "魔塔系统尚未激活，道具已消耗");
                }
            }
            case "xiedaibb30" -> {
                // PHP: permanent +3 pet slots
                WarPlayer wp = warPlayerRepo.findById(playerId).orElse(null);
                if (wp != null) {
                    int cur = wp.getMaxTakePetNum() != null ? wp.getMaxTakePetNum() : 1;
                    wp.setMaxTakePetNum(Math.min(cur + 3, 5));
                    warPlayerRepo.save(wp);
                    result.put("type", "petSlot");
                    result.put("slots", wp.getMaxTakePetNum());
                    result.put("message", "宠物栏位永久+3！当前可携带：" + wp.getMaxTakePetNum() + "只");
                } else {
                    result.put("message", "魔塔系统尚未激活，道具已消耗");
                }
            }
            case "xidian" -> {
                // PHP usedProps.php:1786 — talent wash count
                WarPlayer wp = warPlayerRepo.findById(playerId).orElse(null);
                if (wp != null) {
                    int cur = wp.getWashTalentCount() != null ? wp.getWashTalentCount() : 0;
                    wp.setWashTalentCount(cur + value);
                    warPlayerRepo.save(wp);
                    result.put("type", "talentWash");
                    result.put("washAdded", value);
                    result.put("message", "恭喜您增加了" + value + "次天赋洗点机会！");
                } else {
                    result.put("message", "魔塔系统尚未激活，道具已消耗");
                }
            }
            case "xiedaibb31" -> {
                // PHP: 30 days +3 pet slots
                WarPlayer wp = warPlayerRepo.findById(playerId).orElse(null);
                if (wp != null) {
                    int cur = wp.getMaxTakePetNum() != null ? wp.getMaxTakePetNum() : 1;
                    int limit = wp.getTakePetLimitTime() != null ? wp.getTakePetLimitTime() : 0;
                    wp.setMaxTakePetNum(Math.min(cur + 3, 5));
                    wp.setTakePetLimitTime(limit + 30 * 86400);
                    warPlayerRepo.save(wp);
                    result.put("type", "petSlot");
                    result.put("slots", wp.getMaxTakePetNum());
                    result.put("message", "宠物栏位+3（30天）！当前可携带：" + wp.getMaxTakePetNum() + "只");
                } else {
                    result.put("message", "魔塔系统尚未激活，道具已消耗");
                }
            }
            case "jg" -> {
                // PHP varyname==14: military merit points → battlefield_user.jgvalue
                result.put("type", "militaryMerit");
                result.put("meritAdded", value);
                result.put("message", "恭喜您增加了" + value + "点军功！");
            }
            case "ticket" -> {
                // PHP varyname==4: lottery ticket → ticket_YYYYMMDD table
                result.put("type", "lottery");
                result.put("message", "彩票已记录，等待开奖！");
            }
            case "needkey" -> {
                // Chest requires key — handled at higher level (chest parsing)
                result.put("type", "needkey");
                result.put("keyPropId", value);
            }
            case "giveitems" -> {
                // Fixed-reward chest: format "propId:count:flag,propId:count:flag,..."
                // Parsed at a higher level since value is just the first number
                result.put("type", "giveitems");
            }
            case "randitem" -> {
                // Random-reward chest: format "propId:count:prob:flag|propId:count:prob:flag|..."
                result.put("type", "randitem");
            }
            default -> {
                // unknown effect type, skip
            }
        }
    }

    /** PHP usedProps.php: recursive chest opening — if given item is a chest, open it too */
    private void openChestRecursive(Long playerId, long propId, int count, List<Map<String, Object>> givenItems) {
        Props prop = propsRepo.findById(propId).orElse(null);
        if (prop == null || prop.getEffect() == null) return;
        String eff = prop.getEffect();
        if (!eff.startsWith("giveitems:") && !eff.startsWith("randitem:")) return;

        // Recursive chest found — open it (max 3 levels deep to prevent infinite loops)
        String chestBody = eff.substring(eff.indexOf(':') + 1);
        boolean isRandom = eff.startsWith("randitem:");
        String[] items = isRandom ? chestBody.split("\\|") : chestBody.split(",");
        int depth = 0;
        for (String itemStr : items) {
            if (depth >= 3) break;
            String[] parts = itemStr.split(":");
            if (parts.length < 2) continue;
            try {
                long childPropId = Long.parseLong(parts[0].trim());
                int childCount = Integer.parseInt(parts[1].trim());
                if (isRandom) {
                    int prob = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 100;
                    if ((int)(Math.random() * prob) >= 1) continue;
                    addItemToBag(playerId, childPropId, childCount);
                    givenItems.add(Map.of("propId", childPropId, "count", childCount, "fromChest", true));
                    break;
                } else {
                    addItemToBag(playerId, childPropId, childCount);
                    givenItems.add(Map.of("propId", childPropId, "count", childCount, "fromChest", true));
                }
                depth++;
            } catch (NumberFormatException ignored) {}
        }
    }

    /** PHP: useItem by prop ID (usedProps.php?pid=X&js) */
    public Map<String, Object> useItemByPid(Long playerId, Long propId, boolean isJs) {
        var bagItem = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(propId) && b.getSums() != null && b.getSums() > 0)
            .findFirst().orElse(null);
        if (bagItem == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "无相关魔法石，无法满足释放魔法需要的魔力T_T下次再来吧。");
            return error;
        }
        return useItem(playerId, bagItem.getId(), 0L, isJs);
    }

    /** PHP getBagOfVary.php — get all magic stone types (varyname=22) */
    public List<Map<String, Object>> getMagicStoneTypes() {
        return propsRepo.findAll().stream()
            .filter(p -> p.getVaryname() != null && p.getVaryname() == 22)
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId()); m.put("name", s.getName()); m.put("effect", s.getEffect());
                return m;
            }).collect(Collectors.toList());
    }

    private void addItemToBag(Long playerId, long propId, int count) {
        List<UserBag> existing = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId() == propId && (b.getVary() == null || b.getVary() == 1))
            .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            UserBag bag = existing.get(0);
            bag.setSums((bag.getSums() != null ? bag.getSums() : 0) + count);
            bagRepo.save(bag);
        } else {
            UserBag bag = new UserBag();
            bag.setPlayerId(playerId);
            bag.setPropId(propId);
            bag.setSums(count);
            bag.setVary(1);
            bag.setStime(System.currentTimeMillis() / 1000);
            bagRepo.save(bag);
        }
    }

    private void decrementOrRemove(UserBag item) {
        int count = item.getSums() != null ? item.getSums() : 0;
        if (count <= 1) {
            bagRepo.delete(item);
        } else {
            item.setSums(count - 1);
            bagRepo.save(item);
        }
    }

    /** 10 equipment slot names matching PHP position 0-10 */
    private static final String[] SLOT_NAMES = {"武器","衣服","头盔","鞋子","项链","戒指左","戒指右","护腕","腰带","特殊","翅膀"};
    /** Item category names by varyname */
    public static final Map<Integer, String> CATEGORIES = Map.ofEntries(
        Map.entry(0, "普通"), Map.entry(1, "辅助"), Map.entry(2, "增益"), Map.entry(3, "捕捉"),
        Map.entry(4, "收集"), Map.entry(5, "技能书"), Map.entry(6, "卡片"), Map.entry(7, "进化"),
        Map.entry(8, "合体"), Map.entry(9, "装备"), Map.entry(10, "精练"),
        Map.entry(11, "宝箱"), Map.entry(12, "特殊"), Map.entry(13, "功能"),
        Map.entry(14, "宠物卵"), Map.entry(15, "合成"), Map.entry(20, "传承")
    );

    /**
     * Equip a piece of equipment to a pet. 10-slot system matching PHP usedProps.php.
     * Replaces same-position equipment and clears old one.
     */
    @Transactional
    public Map<String, Object> equipItem(Long playerId, Long bagItemId, Long petId) {
        UserBag bagItem = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("装备不存在"));
        if (!bagItem.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的装备");
        if (bagItem.getVary() == null || bagItem.getVary() != 2)
            throw new IllegalArgumentException("该物品不能穿戴");

        UserPet pet = userPetRepo.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        if (!pet.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的宠物");

        Props props = propsRepo.findById(bagItem.getPropId().longValue()).orElse(null);
        if (props == null) throw new IllegalArgumentException("道具定义不存在");

        // varyname=9 means equipment; other varyname (5=skill book etc.) are not
        if (props.getVaryname() == null || props.getVaryname() != 9)
            throw new IllegalArgumentException("该物品不是装备（是技能书或其他道具）");

        // Check level/wx requirements
        String requires = props.getRequires();
        if (requires != null && !requires.isEmpty()) {
            for (String part : requires.split(",")) {
                String[] kv = part.split(":");
                if (kv.length < 2) continue;
                if ("lv".equals(kv[0])) {
                    int reqLv = Integer.parseInt(kv[1]);
                    if ((pet.getLevel() != null ? pet.getLevel() : 1) < reqLv)
                        throw new IllegalArgumentException("宠物等级不足，需要" + reqLv + "级");
                } else if ("wx".equals(kv[0]) && !kv[1].isEmpty()) {
                    int reqWx = Integer.parseInt(kv[1]);
                    if (!Integer.valueOf(reqWx).equals(pet.getWx()))
                        throw new IllegalArgumentException("宠物五行不匹配");
                }
            }
        }

        int position = props.getPostion() != null ? props.getPostion() : 0;
        if (position < 0 || position > 10) position = 0;

        // Get current zb map
        Map<Integer, Long> zbMap = parseZb(pet.getZb());
        Long oldBagId = zbMap.get(position);

        // If same item already equipped at this position, nothing to do
        if (oldBagId != null && oldBagId.equals(bagItemId)) {
            throw new IllegalArgumentException("该装备已穿戴在此位置");
        }

        // Clear old equipment at this position
        if (oldBagId != null) {
            UserBag oldItem = bagRepo.findById(oldBagId).orElse(null);
            if (oldItem != null) {
                oldItem.setZbing(0);
                oldItem.setEquipPetId(null);
                bagRepo.save(oldItem);
                applyEquipmentStats(pet, oldItem.getPropId(), false);
            }
        }

        // Set new equipment
        zbMap.put(position, bagItemId);
        pet.setZb(zbMapToString(zbMap));
        userPetRepo.save(pet);

        bagItem.setZbing(1);
        bagItem.setEquipPetId(petId);
        bagRepo.save(bagItem);

        applyEquipmentStats(pet, bagItem.getPropId(), true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipped", true);
        result.put("position", position);
        result.put("slotName", position < SLOT_NAMES.length ? SLOT_NAMES[position] : "未知");
        result.put("propName", props.getName());
        result.put("replaced", oldBagId != null);
        result.put("zb", pet.getZb());
        return result;
    }

    /**
     * Unequip an item. Removes from pet's zb string and clears equip flags.
     */
    @Transactional
    public Map<String, Object> unequipItem(Long playerId, Long bagItemId) {
        UserBag bagItem = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("装备不存在"));
        if (!bagItem.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的装备");

        // PHP: check bag space before unequipping
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        int maxBag = player != null && player.getMaxBag() != null ? player.getMaxBag() : 30;
        long bagCount = bagRepo.findByPlayerId(playerId).stream()
            .filter(i -> i.getSums() != null && i.getSums() > 0 && (i.getZbing() == null || i.getZbing() == 0))
            .count();
        if (bagCount >= maxBag)
            throw new IllegalArgumentException("包裹已满，请先清理包裹！");

        Long petId = bagItem.getEquipPetId();
        int position = -1;

        if (petId != null) {
            UserPet pet = userPetRepo.findById(petId).orElse(null);
            if (pet != null) {
                Map<Integer, Long> zbMap = parseZb(pet.getZb());
                for (Map.Entry<Integer, Long> e : zbMap.entrySet()) {
                    if (e.getValue().equals(bagItemId)) { position = e.getKey(); break; }
                }
                if (position >= 0) {
                    zbMap.remove(position);
                    pet.setZb(zbMapToString(zbMap));
                    userPetRepo.save(pet);
                }
                applyEquipmentStats(pet, bagItem.getPropId(), false);
            }
        }

        bagItem.setZbing(0);
        bagItem.setEquipPetId(null);
        bagRepo.save(bagItem);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unequipped", true);
        result.put("position", position);
        result.put("slotName", position >= 0 && position < SLOT_NAMES.length ? SLOT_NAMES[position] : null);
        return result;
    }

    /**
     * Get all equipment stat bonuses for a pet. Called during battle.
     * Returns map: ac/mc/hits/speed/miss/addhp/addmp/crit
     */
    public Map<String, Long> getEquipmentBonuses(Long petId) {
        Map<String, Long> bonuses = new LinkedHashMap<>();
        bonuses.put("ac", 0L); bonuses.put("mc", 0L); bonuses.put("hits", 0L);
        bonuses.put("speed", 0L); bonuses.put("miss", 0L); bonuses.put("hp", 0L);
        bonuses.put("mp", 0L); bonuses.put("crit", 0L);

        UserPet pet = userPetRepo.findById(petId).orElse(null);
        if (pet == null || pet.getZb() == null || pet.getZb().isEmpty()) return bonuses;

        Map<Integer, Long> zbMap = parseZb(pet.getZb());
        for (Long bagId : zbMap.values()) {
            UserBag item = bagRepo.findById(bagId).orElse(null);
            if (item == null || item.getPropId() == null) continue;
            Props props = propsRepo.findById(item.getPropId().longValue()).orElse(null);
            if (props == null || props.getEffect() == null) continue;
            for (String part : props.getEffect().split(",")) {
                String[] kv = part.split(":");
                if (kv.length < 2) continue;
                try {
                    long v = Long.parseLong(kv[1].trim());
                    bonuses.merge(kv[0].trim(), v, Long::sum);
                } catch (NumberFormatException ignored) {}
            }
        }
        return bonuses;
    }

    /** Parse zb string "pos:bagId,pos:bagId" → Map */
    private Map<Integer, Long> parseZb(String zb) {
        Map<Integer, Long> map = new LinkedHashMap<>();
        if (zb == null || zb.isEmpty()) return map;
        for (String pair : zb.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length >= 2) {
                try { map.put(Integer.parseInt(kv[0]), Long.parseLong(kv[1])); }
                catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    private String zbMapToString(Map<Integer, Long> map) {
        if (map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Long> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    /** Apply(+) or remove(-) equipment stats to a pet */
    /** Sell a bag item for gold */
    @Transactional
    public Map<String, Object> sellItem(Long playerId, Long bagItemId, int count) {
        UserBag item = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId))
            throw new IllegalArgumentException("不是你的物品");
        if (item.getZbing() != null && item.getZbing() == 1)
            throw new IllegalArgumentException("已装备的物品不能出售");

        int current = item.getSums() != null ? item.getSums() : 0;
        int toSell = Math.min(count, current);
        Props props = propsRepo.findById(item.getPropId().longValue()).orElse(null);
        int sellPrice = props != null && props.getSell() != null ? props.getSell() : 0;
        int totalGold = sellPrice * toSell;

        if (toSell >= current) bagRepo.delete(item);
        else { item.setSums(current - toSell); bagRepo.save(item); }

        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        if (player != null) {
            int newMoney = (player.getMoney() != null ? player.getMoney() : 0) + totalGold;
            player.setMoney(Math.min(newMoney, 1_000_000_000)); // cap 1 billion
            playerRepo.save(player);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sold", props != null ? props.getName() : "物品");
        result.put("count", toSell);
        result.put("goldGained", totalGold);
        return result;
    }

    private void applyEquipmentStats(UserPet pet, Long propId, boolean apply) {
        Props props = propsRepo.findById(propId).orElse(null);
        if (props == null || props.getEffect() == null || props.getEffect().isEmpty()) return;
        long sign = apply ? 1 : -1;
        for (String part : props.getEffect().split(",")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            long value;
            try { value = Long.parseLong(kv[1].trim()); } catch (NumberFormatException e) { continue; }
            long delta = value * sign;
            switch (kv[0].trim()) {
                case "ac" -> pet.setAc((pet.getAc() != null ? pet.getAc() : 0) + delta);
                case "mc" -> pet.setMc((pet.getMc() != null ? pet.getMc() : 0) + delta);
                case "hp" -> {
                    pet.setAddhp((pet.getAddhp() != null ? pet.getAddhp() : 0) + delta);
                    if (apply) pet.setHp((pet.getHp() != null ? pet.getHp() : 0) + value);
                    else pet.setHp(Math.max(1, (pet.getHp() != null ? pet.getHp() : 0) + delta));
                }
                case "mp" -> {
                    pet.setAddmp((pet.getAddmp() != null ? pet.getAddmp() : 0) + delta);
                    if (apply) pet.setMp((pet.getMp() != null ? pet.getMp() : 0) + value);
                    else pet.setMp(Math.max(0, (pet.getMp() != null ? pet.getMp() : 0) + delta));
                }
                case "speed" -> pet.setSpeed((pet.getSpeed() != null ? pet.getSpeed() : 0) + delta);
                case "hits" -> pet.setHits((pet.getHits() != null ? pet.getHits() : 0) + delta);
                case "miss" -> pet.setMiss((pet.getMiss() != null ? pet.getMiss() : 0) + delta);
            }
        }
        userPetRepo.save(pet);
    }
}
