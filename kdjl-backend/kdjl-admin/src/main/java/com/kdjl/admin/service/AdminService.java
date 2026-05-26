package com.kdjl.admin.service;

import com.kdjl.common.entity.*;
import com.kdjl.admin.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final AdminPlayerRepository playerRepo;
    private final AdminPlayerExtRepository playerExtRepo;
    private final AdminUserPetRepository petRepo;
    private final AdminUserBagRepository bagRepo;
    private final AdminPropsRepository propsRepo;
    private final AdminPetRepository petTemplateRepo;
    private final AdminFightLogRepository fightLogRepo;
    private final AdminYbLogRepository ybLogRepo;

    public AdminService(AdminPlayerRepository playerRepo, AdminPlayerExtRepository playerExtRepo,
                        AdminUserPetRepository petRepo, AdminUserBagRepository bagRepo,
                        AdminPropsRepository propsRepo, AdminPetRepository petTemplateRepo,
                        AdminFightLogRepository fightLogRepo, AdminYbLogRepository ybLogRepo) {
        this.playerRepo = playerRepo;
        this.playerExtRepo = playerExtRepo;
        this.petRepo = petRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.petTemplateRepo = petTemplateRepo;
        this.fightLogRepo = fightLogRepo;
        this.ybLogRepo = ybLogRepo;
    }

    // ---- Dashboard stats ----
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalPlayers = playerRepo.count();
        int fiveMinAgo = (int)(System.currentTimeMillis() / 1000) - 300;
        long onlineNow = playerRepo.countOnlineSince(fiveMinAgo);
        int todayStart = (int)(System.currentTimeMillis() / 1000) - 86400;
        long todayActive = playerRepo.countOnlineSince(todayStart);
        long totalPets = petRepo.count();
        long totalProps = propsRepo.count();
        stats.put("totalPlayers", totalPlayers);
        stats.put("onlineNow", onlineNow);
        stats.put("todayActive", todayActive);
        stats.put("totalPets", totalPets);
        stats.put("totalProps", totalProps);

        // 7-day activity trend
        List<Map<String, Object>> trend = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000;
        for (int i = 6; i >= 0; i--) {
            long dayStart = now - (i + 1) * 86400L;
            long dayEnd = now - i * 86400L;
            long count = playerRepo.countOnlineBetween((int) dayStart, (int) dayEnd);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", new java.text.SimpleDateFormat("MM/dd").format(new java.util.Date(dayEnd * 1000)));
            day.put("count", count);
            trend.add(day);
        }
        stats.put("trend", trend);

        // Server info
        Runtime rt = Runtime.getRuntime();
        stats.put("serverUptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        stats.put("heapUsed", (rt.totalMemory() - rt.freeMemory()) / 1048576);
        stats.put("heapMax", rt.maxMemory() / 1048576);
        stats.put("dbStatus", "connected");

        return stats;
    }

    // ---- Player search ----
    public List<Map<String, Object>> searchPlayers(String keyword, int page, int size) {
        return playerRepo.findAll().stream()
            .filter(p -> keyword == null || keyword.isEmpty() ||
                (p.getNickname() != null && p.getNickname().contains(keyword)) ||
                (p.getUsername() != null && p.getUsername().contains(keyword)))
            .skip((long) (page - 1) * size)
            .limit(size)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("username", p.getUsername());
                m.put("nickname", p.getNickname());
                m.put("level", p.getScore() != null ? p.getScore() : 0);
                m.put("money", p.getMoney() != null ? p.getMoney() : 0);
                m.put("yb", p.getYb() != null ? p.getYb() : 0);
                m.put("prestige", p.getPrestige() != null ? p.getPrestige() : 0);
                int lastVisit = p.getLastVisitTime() != null ? p.getLastVisitTime() : 0;
                m.put("online", lastVisit > (System.currentTimeMillis() / 1000 - 300));
                m.put("regtime", p.getRegtime());
                return m;
            })
            .collect(Collectors.toList());
    }

    public long countPlayers(String keyword) {
        if (keyword == null || keyword.isEmpty()) return playerRepo.count();
        return playerRepo.findAll().stream()
            .filter(p -> (p.getNickname() != null && p.getNickname().contains(keyword)) ||
                (p.getUsername() != null && p.getUsername().contains(keyword)))
            .count();
    }

    // ---- Player detail ----
    public Map<String, Object> getPlayerDetail(Integer playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return null;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", player.getId());
        detail.put("username", player.getUsername());
        detail.put("nickname", player.getNickname());
        detail.put("level", player.getScore() != null ? player.getScore() : 0);
        detail.put("money", player.getMoney() != null ? player.getMoney() : 0);
        detail.put("yb", player.getYb() != null ? player.getYb() : 0);
        detail.put("prestige", player.getPrestige() != null ? player.getPrestige() : 0);
        detail.put("vip", player.getVip());
        detail.put("mbid", player.getMbid());
        detail.put("regtime", player.getRegtime());
        detail.put("lastVisitTime", player.getLastVisitTime());
        detail.put("autoFitFlag", player.getAutoFitFlag());
        detail.put("sysAutoSum", player.getSysAutoSum());
        detail.put("maxAutoFitSum", player.getMaxAutoFitSum());
        detail.put("dblExpFlag", player.getDblExpFlag());

        PlayerExt ext = playerExtRepo.findById(playerId).orElse(null);
        if (ext != null) {
            detail.put("sj", ext.getSj() != null ? ext.getSj() : 0);
            detail.put("tgt", ext.getTgt() != null ? ext.getTgt() : 0);
            detail.put("onlineTime", ext.getOnlineTime() != null ? ext.getOnlineTime() : 0);
        } else {
            detail.put("sj", 0); detail.put("tgt", 0); detail.put("onlineTime", 0);
        }

        // Player's pets
        List<UserPet> pets = petRepo.findByPlayerId(playerId.longValue());
        detail.put("pets", pets.stream().map(p -> {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.getId()); pm.put("name", p.getName());
            pm.put("level", p.getLevel()); pm.put("hp", p.getHp());
            pm.put("czl", p.getCzl()); pm.put("wx", p.getWx());
            pm.put("skillList", p.getSkillList());
            return pm;
        }).collect(Collectors.toList()));

        // Player's bag
        List<UserBag> bag = bagRepo.findByPlayerId(playerId.longValue());
        detail.put("bag", bag.stream().filter(b -> b.getSums() != null && b.getSums() > 0).map(b -> {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("id", b.getId()); bm.put("propId", b.getPropId());
            bm.put("sums", b.getSums()); bm.put("vary", b.getVary());
            Props prop = propsRepo.findById(b.getPropId()).orElse(null);
            bm.put("name", prop != null ? prop.getName() : "未知道具");
            return bm;
        }).collect(Collectors.toList()));

        return detail;
    }

    // ---- Give items ----
    @Transactional
    public Map<String, Object> giveItem(Integer playerId, Long propId, int count) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return Map.of("error", "玩家不存在");
        Props prop = propsRepo.findById(propId).orElse(null);
        if (prop == null) return Map.of("error", "道具不存在");

        UserBag existing = bagRepo.findByPlayerId(playerId.longValue()).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(propId)
                && (b.getVary() == null || b.getVary() != 2))
            .findFirst().orElse(null);
        if (existing != null) {
            existing.setSums((existing.getSums() != null ? existing.getSums() : 0) + count);
            bagRepo.save(existing);
        } else {
            UserBag item = new UserBag();
            item.setPlayerId(playerId.longValue()); item.setPropId(propId); item.setSums(count);
            item.setVary(prop.getVary()); item.setSell(prop.getSell()); item.setZbing(0);
            item.setPyb(0); item.setPsell(0); item.setPstime(0L); item.setBsum(0);
            item.setPetime(0L); item.setPsum(0);
            item.setStime(System.currentTimeMillis() / 1000);
            bagRepo.save(item);
        }
        return Map.of("success", true, "player", player.getNickname(), "prop", prop.getName(), "count", count);
    }

    @Transactional
    public Map<String, Object> giveCurrency(Integer playerId, String type, int amount) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return Map.of("error", "玩家不存在");

        return switch (type) {
            case "money" -> {
                player.setMoney((player.getMoney() != null ? player.getMoney() : 0) + amount);
                playerRepo.save(player);
                yield Map.of("success", true, "type", "金币", "amount", amount);
            }
            case "yb" -> {
                player.setYb((player.getYb() != null ? player.getYb() : 0) + amount);
                playerRepo.save(player);
                yield Map.of("success", true, "type", "元宝", "amount", amount);
            }
            case "sj" -> {
                PlayerExt ext = playerExtRepo.findById(playerId).orElse(null);
                if (ext == null) yield Map.of("error", "玩家扩展数据不存在");
                ext.setSj((ext.getSj() != null ? ext.getSj() : 0) + amount);
                playerExtRepo.save(ext);
                yield Map.of("success", true, "type", "水晶", "amount", amount);
            }
            default -> Map.of("error", "未知货币类型");
        };
    }

    // ---- Props browser ----
    public List<Map<String, Object>> browseProps(String keyword, int page, int size) {
        return propsRepo.findAll().stream()
            .filter(p -> keyword == null || keyword.isEmpty() ||
                (p.getName() != null && p.getName().contains(keyword)))
            .skip((long) (page - 1) * size)
            .limit(size)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId()); m.put("name", p.getName());
                m.put("vary", p.getVary()); m.put("sell", p.getSell());
                m.put("buy", p.getBuy()); m.put("yb", p.getYb());
                m.put("sj", p.getSj()); m.put("effect", p.getEffect());
                m.put("usages", p.getUsages());
                return m;
            }).collect(Collectors.toList());
    }

    public long countProps(String keyword) {
        if (keyword == null || keyword.isEmpty()) return propsRepo.count();
        return propsRepo.findAll().stream()
            .filter(p -> p.getName() != null && p.getName().contains(keyword)).count();
    }

    // ---- Pet template browser ----
    public List<Map<String, Object>> browsePets(String keyword, int page, int size) {
        return petTemplateRepo.findAll().stream()
            .filter(p -> keyword == null || keyword.isEmpty() ||
                (p.getName() != null && p.getName().contains(keyword)))
            .skip((long) (page - 1) * size)
            .limit(size)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId()); m.put("name", p.getName());
                m.put("wx", p.getWx()); m.put("czl", p.getCzl());
                m.put("hp", p.getHp()); m.put("mp", p.getMp());
                m.put("ac", p.getAc()); m.put("mc", p.getMc());
                m.put("skillList", p.getSkillList());
                return m;
            }).collect(Collectors.toList());
    }

    public long countPets(String keyword) {
        if (keyword == null || keyword.isEmpty()) return petTemplateRepo.count();
        return petTemplateRepo.findAll().stream()
            .filter(p -> p.getName() != null && p.getName().contains(keyword)).count();
    }

    // ---- Player ban/mute (PHP: chatGate.php FH/JY/JJ) ----
    @Transactional
    public Map<String, Object> banPlayer(Integer playerId) {
        Player p = playerRepo.findById(playerId).orElse(null);
        if (p == null) return Map.of("error", "玩家不存在");
        p.setSecId(1);
        playerRepo.save(p);
        return Map.of("success", true, "action", "封号", "player", p.getNickname());
    }

    @Transactional
    public Map<String, Object> unbanPlayer(Integer playerId) {
        Player p = playerRepo.findById(playerId).orElse(null);
        if (p == null) return Map.of("error", "玩家不存在");
        p.setSecId(0);
        playerRepo.save(p);
        return Map.of("success", true, "action", "解封", "player", p.getNickname());
    }

    @Transactional
    public Map<String, Object> mutePlayer(Integer playerId, int minutes) {
        Player p = playerRepo.findById(playerId).orElse(null);
        if (p == null) return Map.of("error", "玩家不存在");
        long muteUntil = System.currentTimeMillis() / 1000 + minutes * 60L;
        p.setPassword(String.valueOf(muteUntil)); // PHP: player.password as mute timer
        playerRepo.save(p);
        return Map.of("success", true, "action", "禁言", "player", p.getNickname(), "minutes", minutes);
    }

    @Transactional
    public Map<String, Object> unmutePlayer(Integer playerId) {
        Player p = playerRepo.findById(playerId).orElse(null);
        if (p == null) return Map.of("error", "玩家不存在");
        p.setPassword("0");
        playerRepo.save(p);
        return Map.of("success", true, "action", "解禁", "player", p.getNickname());
    }

    // ---- Battle log ----
    public List<Map<String, Object>> getFightLogs(Integer playerId) {
        return fightLogRepo.findByPlayerIdOrderByTimeDesc(playerId.longValue()).stream()
            .limit(50)
            .map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", f.getId());
                m.put("playerId", f.getPlayerId());
                m.put("time", f.getTime());
                m.put("vary", f.getVary());
                return m;
            }).collect(Collectors.toList());
    }

    // ---- Payment stats ----
    public Map<String, Object> getPaymentStats() {
        List<YbLog> recent = ybLogRepo.findTop100ByOrderByBuytimeDesc();
        List<Object[]> byTitle = ybLogRepo.sumByTitle();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("recent", recent.stream().map(y -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", y.getId()); m.put("title", y.getTitle());
            m.put("nickname", y.getNickname()); m.put("yb", y.getYb());
            m.put("buytime", y.getBuytime()); m.put("pname", y.getPname());
            m.put("nums", y.getNums());
            return m;
        }).collect(Collectors.toList()));

        long totalYb = recent.stream().mapToLong(y -> y.getYb() != null ? y.getYb() : 0).sum();
        stats.put("totalYb", totalYb);
        stats.put("byTitle", byTitle.stream().map(row -> Map.of("title", row[0], "total", row[1])).collect(Collectors.toList()));
        return stats;
    }
}
