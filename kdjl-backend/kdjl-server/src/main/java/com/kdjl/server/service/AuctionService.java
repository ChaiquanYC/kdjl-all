package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuctionService {
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;
    private final PlayerExtRepository playerExtRepo;
    private static final int LIMIT = 60;
    private static final int EXPIRE_SECONDS = 10800;
    private static final int MAX_AUCTIONS = 3;

    public AuctionService(UserBagRepository bagRepo, PropsRepository propsRepo,
                          PlayerRepository playerRepo, PlayerExtRepository playerExtRepo) {
        this.bagRepo = bagRepo; this.propsRepo = propsRepo;
        this.playerRepo = playerRepo; this.playerExtRepo = playerExtRepo;
    }

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
        int price = "sj".equals(type) ? parseIntSafe(b.getPsj())
                  : "yb".equals(type) ? (b.getPyb() != null ? b.getPyb() : 0)
                  : (b.getPsell() != null ? b.getPsell() : 0);
        m.put("price", price);
        if (b.getPetime() != null)
            m.put("timeRemaining", Math.max(0, b.getPetime() - System.currentTimeMillis() / 1000));
        return m;
    }

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

    @Transactional
    public Map<String, Object> listForAuction(Long playerId, Long bagId, int price, String type) {
        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId)) throw new IllegalArgumentException("不是你的物品");
        if (item.getZbing() != null && item.getZbing() == 1) throw new IllegalArgumentException("装备中不能拍卖");
        if (item.getCantrade() != null && item.getCantrade() == 0) throw new IllegalArgumentException("不可交易");

        long myAuctions = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPsum() != null && b.getPsum() > 0).count();
        if (myAuctions >= MAX_AUCTIONS) throw new IllegalArgumentException("最多同时上架" + MAX_AUCTIONS + "件");

        long now = System.currentTimeMillis() / 1000;
        item.setPstime(now); item.setPetime(now + EXPIRE_SECONDS);
        int count = item.getSums() != null ? item.getSums() : 1;
        item.setPsum(count); item.setSums(0);
        if ("sj".equals(type)) { item.setPsj(String.valueOf(price)); item.setPsell(0); item.setPyb(0); }
        else if ("yb".equals(type)) { item.setPyb(price); item.setPsell(0); item.setPsj("0"); }
        else { item.setPsell(price); item.setPsj("0"); item.setPyb(0); }
        bagRepo.save(item);
        return Map.of("listed", true, "price", price, "type", type);
    }

    @Transactional
    public Map<String, Object> buyAuction(Long buyerId, Long bagId, String type) {
        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (item.getPsum() == null || item.getPsum() <= 0) throw new IllegalArgumentException("未在拍卖");
        if (item.getPlayerId().equals(buyerId)) throw new IllegalArgumentException("不能买自己的");
        long now = System.currentTimeMillis() / 1000;
        if (item.getPetime() != null && item.getPetime() < now) throw new IllegalArgumentException("已过期");

        int price = "sj".equals(type) ? parseIntSafe(item.getPsj())
                  : "yb".equals(type) ? (item.getPyb() != null ? item.getPyb() : 0)
                  : (item.getPsell() != null ? item.getPsell() : 0);

        Player buyer = playerRepo.findById(buyerId.intValue()).orElse(null);
        if (buyer == null) throw new IllegalArgumentException("买家不存在");

        if ("sj".equals(type)) {
            PlayerExt ext = playerExtRepo.findById(buyerId.intValue()).orElse(null);
            int cur = ext != null && ext.getSj() != null ? ext.getSj() : 0;
            if (cur < price) throw new IllegalArgumentException("水晶不足");
            ext.setSj(cur - price); playerExtRepo.save(ext);
        } else if ("yb".equals(type)) {
            int cur = buyer.getYb() != null ? buyer.getYb() : 0;
            if (cur < price) throw new IllegalArgumentException("元宝不足");
            buyer.setYb(cur - price); playerRepo.save(buyer);
        } else {
            int cur = buyer.getMoney() != null ? buyer.getMoney() : 0;
            if (cur < price) throw new IllegalArgumentException("金币不足");
            buyer.setMoney(cur - price); playerRepo.save(buyer);
        }

        double feeRate = "yb".equals(type) ? 0.05 : 0.08;
        int sellerGets = (int) Math.round(price * (1 - feeRate));
        Player seller = playerRepo.findById(item.getPlayerId().intValue()).orElse(null);
        if (seller != null) {
            if ("sj".equals(type)) {
                PlayerExt ext = playerExtRepo.findById(seller.getId()).orElse(null);
                if (ext != null) { ext.setSj((ext.getSj()!=null?ext.getSj():0)+sellerGets); playerExtRepo.save(ext); }
            } else if ("yb".equals(type)) {
                seller.setYb((seller.getYb()!=null?seller.getYb():0)+sellerGets); playerRepo.save(seller);
            } else {
                seller.setMoney((seller.getMoney()!=null?seller.getMoney():0)+sellerGets); playerRepo.save(seller);
            }
        }

        UserBag newItem = new UserBag();
        newItem.setPlayerId(buyerId); newItem.setPropId(item.getPropId());
        newItem.setSums(item.getPsum()); newItem.setVary(item.getVary());
        newItem.setSell(item.getSell()); newItem.setZbing(0);
        newItem.setPsell(0); newItem.setPsj("0"); newItem.setPyb(0);
        newItem.setPsum(0); newItem.setPetime(0L); newItem.setPstime(0L);
        newItem.setStime(now); newItem.setCantrade(1);
        bagRepo.save(newItem);

        item.setPsum(0); item.setPsell(0); item.setPsj("0"); item.setPyb(0);
        item.setPetime(0L); item.setPstime(0L); item.setSums(0);
        bagRepo.save(item);
        return Map.of("bought", true, "price", price, "fee", price - sellerGets);
    }

    @Transactional
    public Map<String, Object> cancelAuction(Long playerId, Long bagId) {
        UserBag item = bagRepo.findById(bagId).orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!item.getPlayerId().equals(playerId)) throw new IllegalArgumentException("不是你的物品");
        int psum = item.getPsum() != null ? item.getPsum() : 0;
        item.setSums((item.getSums() != null ? item.getSums() : 0) + psum);
        item.setPsum(0); item.setPsell(0); item.setPsj("0"); item.setPyb(0);
        item.setPetime(0L); item.setPstime(0L);
        bagRepo.save(item);
        return Map.of("cancelled", true);
    }

    public List<Map<String, Object>> myAuctions(Long playerId) {
        return bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPsum() != null && b.getPsum() > 0)
            .map(b -> formatItem(b, "all")).collect(Collectors.toList());
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
