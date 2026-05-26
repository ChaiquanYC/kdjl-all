package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuctionService {
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private static final int LIMIT = 60;
    private static final int EXPIRE_SECONDS = 10800;
    private static final int MAX_AUCTIONS = 10;
    private static final int COOLDOWN_SECONDS = 5;

    // Per-user cooldown tracking
    private final ConcurrentHashMap<Long, Long> lastActionTime = new ConcurrentHashMap<>();

    public AuctionService(UserBagRepository bagRepo, PropsRepository propsRepo,
                          PlayerRepository playerRepo, PlayerExtRepository playerExtRepo) {
        this.bagRepo = bagRepo; this.propsRepo = propsRepo;
        this.playerRepo = playerRepo; this.playerExtRepo = playerExtRepo;
    }

    private void checkCooldown(Long playerId) {
        Long last = lastActionTime.get(playerId);
        long now = System.currentTimeMillis() / 1000;
        if (last != null && (now - last) < COOLDOWN_SECONDS)
            throw new IllegalArgumentException("操作太快，请稍候再试");
        lastActionTime.put(playerId, now);
    }

    // ---- List auctions ----

    public List<Map<String, Object>> listAuctions(String type, Integer varyname) {
        long now = System.currentTimeMillis() / 1000;
        return bagRepo.findAll().stream()
            .filter(b -> b.getPsum() != null && b.getPsum() > 0)
            .filter(b -> b.getPetime() == null || b.getPetime() > now)
            .filter(b -> "sj".equals(type) ? (b.getPsj() != null && !b.getPsj().isEmpty() && parseIntSafe(b.getPsj()) > 0)
                    : "yb".equals(type) ? (b.getPyb() != null && b.getPyb() > 0)
                    : (b.getPsell() != null && b.getPsell() > 0))
            .filter(b -> varyname == null || propsRepo.findById(b.getPropId())
                .map(p -> varyname.equals(p.getVaryname())).orElse(false))
            .sorted((a, b) -> Long.compare(
                b.getPstime() != null ? b.getPstime() : 0,
                a.getPstime() != null ? a.getPstime() : 0))
            .limit(LIMIT).map(b -> formatItem(b, type)).collect(Collectors.toList());
    }

    private Map<String, Object> formatItem(UserBag b, String type) {
        Props p = propsRepo.findById(b.getPropId()).orElse(null);
        Player seller = playerRepo.findById(b.getPlayerId().intValue()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId()); m.put("name", p != null ? p.getName() : "物品");
        m.put("count", b.getPsum()); m.put("varyname", p != null ? p.getVaryname() : null);
        m.put("sellerName", seller != null ? seller.getNickname() : "未知");
        m.put("vary", b.getVary()); // 1=stackable, 2=unique
        int price = "sj".equals(type) ? parseIntSafe(b.getPsj())
                  : "yb".equals(type) ? (b.getPyb() != null ? b.getPyb() : 0)
                  : (b.getPsell() != null ? b.getPsell() : 0);
        m.put("price", price);
        m.put("type", "sj".equals(type) ? "sj" : "yb".equals(type) ? "yb" : "gold");
        if (b.getPetime() != null)
            m.put("timeRemaining", Math.max(0, b.getPetime() - System.currentTimeMillis() / 1000));
        return m;
    }

    // ---- My auctions (determine type per item) ----

    public List<Map<String, Object>> myAuctions(Long playerId) {
        long now = System.currentTimeMillis() / 1000;
        return bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPsum() != null && b.getPsum() > 0)
            .map(b -> {
                String type = (b.getPsj() != null && !b.getPsj().isEmpty() && parseIntSafe(b.getPsj()) > 0) ? "sj"
                            : (b.getPyb() != null && b.getPyb() > 0) ? "yb" : "gold";
                Map<String, Object> m = formatItem(b, type);
                m.put("expired", b.getPetime() != null && b.getPetime() < now);
                return m;
            }).collect(Collectors.toList());
    }

    // ---- Bag for sell ----

    public List<Map<String, Object>> getPlayerBagForSell(Long playerId, Integer varyname) {
        return bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getSums() != null && b.getSums() > 0)
            .filter(b -> b.getZbing() == null || b.getZbing() != 1)
            .filter(b -> varyname == null || propsRepo.findById(b.getPropId())
                .map(p -> varyname.equals(p.getVaryname())).orElse(false))
            .map(b -> {
                Props p = propsRepo.findById(b.getPropId()).orElse(null);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", b.getId()); m.put("name", p != null ? p.getName() : "物品");
                m.put("count", b.getSums()); m.put("varyname", p != null ? p.getVaryname() : null);
                return m;
            }).collect(Collectors.toList());
    }

    // ---- List for auction ----

    @Transactional
    public Map<String, Object> listForAuction(Long playerId, Long bagId, int price, String type, int quantity) {
        checkCooldown(playerId);
        if (price <= 0) throw new IllegalArgumentException("价格必须大于0");
        if (("sj".equals(type) || "yb".equals(type)) && price < 10)
            throw new IllegalArgumentException("水晶/元宝拍卖起拍价格最低为10");
        if (quantity <= 0) throw new IllegalArgumentException("数量必须大于0");

        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId)) throw new IllegalArgumentException("不是你的物品");
        if (item.getZbing() != null && item.getZbing() == 1) throw new IllegalArgumentException("装备中不能拍卖");
        if (item.getCantrade() != null && item.getCantrade() == 0) throw new IllegalArgumentException("不可交易");
        if (item.getSums() == null || item.getSums() < quantity) throw new IllegalArgumentException("数量不足");
        if (item.getPsum() != null && item.getPsum() > 0) throw new IllegalArgumentException("此物品已在拍卖中");

        long myAuctions = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPsum() != null && b.getPsum() > 0).count();
        if (myAuctions >= MAX_AUCTIONS) throw new IllegalArgumentException("最多同时上架" + MAX_AUCTIONS + "件");

        long now = System.currentTimeMillis() / 1000;
        item.setPstime(now); item.setPetime(now + EXPIRE_SECONDS);
        item.setSums(item.getSums() - quantity);
        item.setPsum(quantity);
        if ("sj".equals(type)) { item.setPsj(String.valueOf(price)); item.setPsell(0); item.setPyb(0); }
        else if ("yb".equals(type)) { item.setPyb(price); item.setPsell(0); item.setPsj("0"); }
        else { item.setPsell(price); item.setPsj("0"); item.setPyb(0); }
        bagRepo.save(item);
        return Map.of("listed", true, "price", price, "type", type, "quantity", quantity);
    }

    // ---- Buy auction ----

    @Transactional
    public Map<String, Object> buyAuction(Long buyerId, Long bagId, String type, int quantity) {
        checkCooldown(buyerId);
        if (quantity <= 0) throw new IllegalArgumentException("数量必须大于0");

        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (item.getPsum() == null || item.getPsum() <= 0) throw new IllegalArgumentException("未在拍卖");
        if (item.getPsum() < quantity) throw new IllegalArgumentException("拍卖数量不足");
        if (item.getPlayerId().equals(buyerId)) throw new IllegalArgumentException("不能买自己的");
        long now = System.currentTimeMillis() / 1000;
        if (item.getPetime() != null && item.getPetime() < now) throw new IllegalArgumentException("已过期");

        int price = "sj".equals(type) ? parseIntSafe(item.getPsj())
                  : "yb".equals(type) ? (item.getPyb() != null ? item.getPyb() : 0)
                  : (item.getPsell() != null ? item.getPsell() : 0);
        int totalPrice = price * quantity;

        // Check buycode (CRC32 nickname restriction)
        if (item.getBuyCode() != null && item.getBuyCode() > 1) {
            Player buyer = playerRepo.findById(buyerId.intValue()).orElse(null);
            if (buyer != null) {
                long crc = crc32(buyer.getNickname());
                if (crc != item.getBuyCode())
                    throw new IllegalArgumentException("该物品不是卖给你的哦");
            }
        }

        // Check bag space
        long bagCount = bagRepo.findByPlayerId(buyerId).stream()
            .filter(b -> b.getSums() != null && b.getSums() > 0 && (b.getZbing() == null || b.getZbing() == 0))
            .count();
        Player buyer = playerRepo.findById(buyerId.intValue()).orElseThrow(() -> new IllegalArgumentException("买家不存在"));
        int maxBag = buyer.getMaxBag() != null ? buyer.getMaxBag() : 30;

        // For unique items (vary==2), each purchase takes 1 bag slot
        // For stackable items (vary==1), only takes a slot if buyer doesn't already have it
        boolean alreadyHas = false;
        if (item.getVary() != null && item.getVary() == 1) {
            alreadyHas = bagRepo.findByPlayerId(buyerId).stream()
                .anyMatch(b -> b.getPropId().equals(item.getPropId()) && b.getSums() != null && b.getSums() > 0
                           && (b.getZbing() == null || b.getZbing() == 0));
        }
        if (!alreadyHas && bagCount + 1 > maxBag) throw new IllegalArgumentException("包裹空间不足");

        // Deduct from buyer
        if ("sj".equals(type)) {
            PlayerExt ext = playerExtRepo.findById(buyerId.intValue()).orElse(null);
            int cur = ext != null && ext.getSj() != null ? ext.getSj() : 0;
            if (cur < totalPrice) throw new IllegalArgumentException("水晶不足");
            ext.setSj(cur - totalPrice); playerExtRepo.save(ext);
        } else if ("yb".equals(type)) {
            int cur = buyer.getYb() != null ? buyer.getYb() : 0;
            if (cur < totalPrice) throw new IllegalArgumentException("元宝不足");
            buyer.setYb(cur - totalPrice); playerRepo.save(buyer);
        } else {
            int cur = buyer.getMoney() != null ? buyer.getMoney() : 0;
            if (cur < totalPrice) throw new IllegalArgumentException("金币不足");
            buyer.setMoney(cur - totalPrice); playerRepo.save(buyer);
        }

        // Seller receives in ESCROW (paimoney/paisj/paiyb), not directly
        double feeRate = "yb".equals(type) ? 0.05 : 0.08;
        int sellerGets = (int) Math.round(totalPrice * (1 - feeRate));
        Player seller = playerRepo.findById(item.getPlayerId().intValue()).orElse(null);
        if (seller != null) {
            if ("sj".equals(type)) {
                PlayerExt ext = playerExtRepo.findById(seller.getId()).orElse(null);
                if (ext != null) {
                    ext.setPaisj((ext.getPaisj() != null ? ext.getPaisj() : 0) + sellerGets);
                    playerExtRepo.save(ext);
                }
            } else if ("yb".equals(type)) {
                PlayerExt ext = playerExtRepo.findById(seller.getId()).orElse(null);
                if (ext != null) {
                    ext.setPaiyb((ext.getPaiyb() != null ? ext.getPaiyb() : 0) + sellerGets);
                    playerExtRepo.save(ext);
                }
            } else {
                seller.setPaiMoney((seller.getPaiMoney() != null ? seller.getPaiMoney() : 0) + sellerGets);
                playerRepo.save(seller);
            }
        }

        // Give item to buyer — merge if stackable
        if (item.getVary() != null && item.getVary() == 1) {
            UserBag existing = bagRepo.findByPlayerId(buyerId).stream()
                .filter(b -> b.getPropId().equals(item.getPropId()) && b.getSums() != null && b.getSums() > 0
                           && (b.getZbing() == null || b.getZbing() == 0))
                .findFirst().orElse(null);
            if (existing != null) {
                existing.setSums(existing.getSums() + quantity);
                bagRepo.save(existing);
            } else {
                UserBag newItem = new UserBag();
                newItem.setPlayerId(buyerId); newItem.setPropId(item.getPropId());
                newItem.setSums(quantity); newItem.setVary(item.getVary());
                newItem.setSell(item.getSell()); newItem.setZbing(0);
                newItem.setPsell(0); newItem.setPsj("0"); newItem.setPyb(0);
                newItem.setPsum(0); newItem.setPetime(0L); newItem.setPstime(0L);
                newItem.setStime(now); newItem.setCantrade(1);
                bagRepo.save(newItem);
            }
        } else {
            // Unique item (vary==2): transfer directly
            UserBag newItem = new UserBag();
            newItem.setPlayerId(buyerId); newItem.setPropId(item.getPropId());
            newItem.setSums(quantity); newItem.setVary(item.getVary());
            newItem.setSell(item.getSell()); newItem.setZbing(0);
            newItem.setPsell(0); newItem.setPsj("0"); newItem.setPyb(0);
            newItem.setPsum(0); newItem.setPetime(0L); newItem.setPstime(0L);
            newItem.setStime(now); newItem.setCantrade(1);
            bagRepo.save(newItem);
        }

        // Update seller's auction item
        if (item.getPsum() > quantity) {
            item.setPsum(item.getPsum() - quantity);
        } else {
            item.setPsum(0); item.setPsell(0); item.setPsj("0"); item.setPyb(0);
            item.setPetime(0L); item.setPstime(0L);
        }
        bagRepo.save(item);

        return Map.of("bought", true, "totalPrice", totalPrice, "fee", totalPrice - sellerGets);
    }

    // ---- Cancel auction ----

    @Transactional
    public Map<String, Object> cancelAuction(Long playerId, Long bagId) {
        checkCooldown(playerId);
        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId)) throw new IllegalArgumentException("不是你的物品");
        // PHP: cannot cancel within 5 minutes (300s) of listing
        if (item.getPstime() != null) {
            long elapsed = System.currentTimeMillis() / 1000 - item.getPstime();
            if (elapsed < 300) throw new IllegalArgumentException("拍卖物品5分钟内不能取回");
        }
        int psum = item.getPsum() != null ? item.getPsum() : 0;
        item.setSums((item.getSums() != null ? item.getSums() : 0) + psum);
        item.setPsum(0); item.setPsell(0); item.setPsj("0"); item.setPyb(0);
        item.setPetime(0L); item.setPstime(0L);
        bagRepo.save(item);
        return Map.of("cancelled", true);
    }

    // ---- Renew expired auction ----

    @Transactional
    public Map<String, Object> renewAuction(Long playerId, Long bagId) {
        checkCooldown(playerId);
        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId)) throw new IllegalArgumentException("不是你的物品");
        if (item.getPsum() == null || item.getPsum() <= 0) throw new IllegalArgumentException("未在拍卖");
        long now = System.currentTimeMillis() / 1000;
        if (item.getPetime() != null && item.getPetime() >= now) throw new IllegalArgumentException("拍卖尚未过期");
        item.setPstime(now); item.setPetime(now + EXPIRE_SECONDS);
        bagRepo.save(item);
        return Map.of("renewed", true);
    }

    // ---- Withdraw escrow money ----

    @Transactional
    public Map<String, Object> withdrawMoney(Long playerId) {
        checkCooldown(playerId);
        Player player = playerRepo.findById(playerId.intValue()).orElse(null);
        PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
        if (player == null) throw new IllegalArgumentException("玩家不存在");

        int pm = player.getPaiMoney() != null ? player.getPaiMoney() : 0;
        int psj = ext != null && ext.getPaisj() != null ? ext.getPaisj() : 0;
        int pyb = ext != null && ext.getPaiyb() != null ? ext.getPaiyb() : 0;
        if (pm <= 0 && psj <= 0 && pyb <= 0) throw new IllegalArgumentException("没有可提取的资金");

        if (pm > 0) {
            player.setMoney((player.getMoney() != null ? player.getMoney() : 0) + pm);
            player.setPaiMoney(0);
            playerRepo.save(player);
        }
        if (psj > 0 && ext != null) {
            ext.setSj((ext.getSj() != null ? ext.getSj() : 0) + psj);
            ext.setPaisj(0);
            playerExtRepo.save(ext);
        }
        if (pyb > 0 && ext != null) {
            // paiyb is on player_ext; yb is on player
            player.setYb((player.getYb() != null ? player.getYb() : 0) + pyb);
            ext.setPaiyb(0);
            playerRepo.save(player);
            playerExtRepo.save(ext);
        }
        return Map.of("withdrawn", true, "money", pm, "sj", psj, "yb", pyb);
    }

    // ---- Helpers ----

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /** CRC32 matching PHP's crc32() function (signed 32-bit) */
    private long crc32(String input) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long val = crc.getValue();
        // PHP crc32 returns signed 32-bit
        if (val > Integer.MAX_VALUE) val = val - 0x100000000L;
        if (val < 0) val = 1 - val - 1; // PHP: $buycode<0 ? (1-$buycode-1) : $buycode — this makes it positive
        return val;
    }
}
