package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShopService {

    private final PropsRepository propsRepo;
    private final PlayerRepository playerRepo;
    private final UserBagRepository bagRepo;
    private final ShopOrderRepository orderRepo;
    private final PlayerExtRepository playerExtRepo;

    public ShopService(PropsRepository propsRepo, PlayerRepository playerRepo,
                       UserBagRepository bagRepo, ShopOrderRepository orderRepo,
                       PlayerExtRepository playerExtRepo) {
        this.propsRepo = propsRepo;
        this.playerRepo = playerRepo;
        this.bagRepo = bagRepo;
        this.orderRepo = orderRepo;
        this.playerExtRepo = playerExtRepo;
    }

    /** PHP shop types: props(金币道具), equip(金币装备), prestige(威望道具), zprestige(威望装备), yb(元宝), sj(灵石), vip */
    public List<Map<String, Object>> listShopItems(String shopType) {
        List<Props> all = propsRepo.findAll();
        return all.stream()
            .filter(p -> {
                if (p.getId() == null || p.getId() <= 0) return false;
                Integer buy = p.getBuy(); Integer yb = p.getYb();
                Integer prestige = p.getPrestige(); Integer sj = p.getSj();
                Integer varyname = p.getVaryname(); Long stime = p.getStime();
                return switch (shopType != null ? shopType : "props") {
                    case "equip" -> buy != null && buy > 0 && (yb == null || yb == 0) && (varyname != null && varyname == 9);
                    case "prestige" -> prestige != null && prestige > 0 && (varyname == null || varyname != 9);
                    case "zprestige" -> prestige != null && prestige > 0 && (varyname != null && varyname == 9);
                    case "yb" -> yb != null && yb > 0;
                    case "sj" -> sj != null && sj > 0;
                    case "vip" -> false;
                    case "smshop" -> stime != null && stime > 0;
                    default -> buy != null && buy > 0 && (yb == null || yb == 0) && (varyname == null || varyname != 9); // props
                };
            })
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("buy", p.getBuy());
                m.put("yb", p.getYb());
                m.put("sj", p.getSj());
                m.put("stime", p.getStime());
                m.put("prestige", p.getPrestige());
                m.put("img", p.getImg());
                m.put("effect", p.getEffect());
                m.put("vary", p.getVary());
                m.put("varyname", p.getVaryname());
                m.put("postion", p.getPostion());
                m.put("requires", p.getRequires());
                m.put("propsColor", p.getPropscolor());
                m.put("pluseffect", p.getPluseffect());
                m.put("plusflag", p.getPlusflag());
                m.put("usages", p.getUsages());
                m.put("sell", p.getSell());
                int cat = p.getVaryname() != null ? p.getVaryname() : 0;
                m.put("category", BagService.CATEGORIES.getOrDefault(cat, "其他" + cat));
                return m;
            }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> buyItem(Long playerId, Long propsId, int count, String currency) {
        if (count < 1) count = 1;

        Props props = propsRepo.findById(propsId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        Player player = playerRepo.findById(playerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        int totalCost;
        if ("sj".equals(currency)) {
            Integer price = props.getSj();
            if (price == null || price <= 0) throw new IllegalArgumentException("该商品不支持水晶购买");
            totalCost = price * count;
            PlayerExt ext = playerExtRepo.findById(playerId.intValue()).orElse(null);
            int curSj = ext != null && ext.getSj() != null ? ext.getSj() : 0;
            if (curSj < totalCost) throw new IllegalArgumentException("水晶不足");
            if (ext == null) { ext = new PlayerExt(); ext.setPlayerId(playerId.intValue()); }
            ext.setSj(curSj - totalCost);
            playerExtRepo.save(ext);
        } else if ("prestige".equals(currency)) {
            Integer price = props.getPrestige();
            if (price == null || price <= 0) throw new IllegalArgumentException("该商品不支持威望购买");
            totalCost = price * count;
            int curPrestige = player.getPrestige() != null ? player.getPrestige() : 0;
            if (curPrestige < totalCost) throw new IllegalArgumentException("威望不足");
            player.setPrestige(curPrestige - totalCost);
        } else if ("yb".equals(currency)) {
            Integer price = props.getYb();
            if (price == null || price <= 0) throw new IllegalArgumentException("该商品不支持元宝购买");
            totalCost = price * count;
            int currentYb = player.getYb() != null ? player.getYb() : 0;
            if (currentYb < totalCost) throw new IllegalArgumentException("元宝不足");
            player.setYb(currentYb - totalCost);
        } else {
            Integer price = props.getBuy();
            if (price == null || price <= 0) throw new IllegalArgumentException("该商品不支持金币购买");
            totalCost = price * count;
            int currentMoney = player.getMoney() != null ? player.getMoney() : 0;
            if (currentMoney < totalCost) throw new IllegalArgumentException("金币不足");
            player.setMoney(currentMoney - totalCost);
        }
        playerRepo.save(player);

        // Add to bag
        List<UserBag> existingItems = bagRepo.findByPlayerId(playerId).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().longValue() == propsId.longValue()
                && (b.getVary() == null || b.getVary() != 2))
            .collect(Collectors.toList());

        UserBag bagItem;
        if (!existingItems.isEmpty()) {
            bagItem = existingItems.get(0);
            int currentSums = bagItem.getSums() != null ? bagItem.getSums() : 0;
            bagItem.setSums(currentSums + count);
            bagRepo.save(bagItem);
        } else {
            bagItem = new UserBag();
            bagItem.setPlayerId(playerId);
            bagItem.setPropId(propsId);
            bagItem.setSums(count);
            bagItem.setVary(props.getVary());
            bagItem.setSell(props.getSell());
            bagItem.setZbing(0);
            bagItem.setCantrade(1);
            bagItem.setStime(System.currentTimeMillis() / 1000);
            bagItem.setPyb(0);
            bagItem.setPsell(0);
            bagItem.setPstime(0L);
            bagItem.setBsum(0);
            bagItem.setPetime(0L);
            bagItem.setPsum(0);
            bagRepo.save(bagItem);
        }

        // Record order
        ShopOrder order = new ShopOrder();
        order.setPlayerId(playerId);
        order.setUname(player.getNickname());
        order.setPropId(propsId);
        order.setPnum(count);
        order.setFee(totalCost);
        order.setCreateTime(System.currentTimeMillis() / 1000);
        order.setFlag(currency.equals("yb") ? 1 : 0);
        orderRepo.save(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchased", props.getName());
        result.put("count", count);
        result.put("cost", totalCost);
        result.put("currency", currency);
        result.put("remainingMoney", player.getMoney());
        result.put("remainingYb", player.getYb());
        return result;
    }
}
