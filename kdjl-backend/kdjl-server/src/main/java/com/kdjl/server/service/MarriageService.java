package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MarriageService {

    private final PlayerRepository playerRepo;
    private final PlayerExtRepository extRepo;
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;

    private static final int DIVORCE_COST = 2000;
    private static final int DIVORCE_COOLDOWN = 86400; // 24h

    public MarriageService(PlayerRepository playerRepo, PlayerExtRepository extRepo,
                           UserBagRepository bagRepo, PropsRepository propsRepo) {
        this.playerRepo = playerRepo;
        this.extRepo = extRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
    }

    private PlayerExt getOrCreateExt(Integer playerId) {
        return extRepo.findById(playerId).orElseGet(() -> {
            PlayerExt ext = new PlayerExt();
            ext.setPlayerId(playerId);
            ext.setSj(0);
            ext.setMerge(0);
            ext.setRequestMerge(0);
            ext.setRequest(0);
            return extRepo.save(ext);
        });
    }

    /** Propose: send gift item to target player. Gift must have merge=1 flag in props. */
    @Transactional
    public Map<String, Object> propose(Integer playerId, Long targetPlayerId, Long bagItemId, int count) {
        if (playerId.equals(targetPlayerId.intValue()))
            throw new IllegalArgumentException("不能向自己求婚");

        Player target = playerRepo.findById(targetPlayerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("对方不存在"));

        PlayerExt myExt = getOrCreateExt(playerId);
        if (myExt.getMerge() != null && myExt.getMerge() > 0)
            throw new IllegalArgumentException("你已结婚，请先离婚");
        if (myExt.getRequestMerge() != null && myExt.getRequestMerge() > 0)
            throw new IllegalArgumentException("你已向别人求婚，请先取消");

        PlayerExt targetExt = getOrCreateExt(targetPlayerId.intValue());
        if (targetExt.getMerge() != null && targetExt.getMerge() > 0)
            throw new IllegalArgumentException("对方已结婚");

        // Validate gift item (merge=1 flag)
        UserBag gift = bagRepo.findById(bagItemId)
            .orElseThrow(() -> new IllegalArgumentException("物品不存在"));
        if (!gift.getPlayerId().equals(playerId.longValue()))
            throw new IllegalArgumentException("不是你的物品");

        Props props = propsRepo.findById(gift.getPropId().longValue()).orElse(null);
        if (props == null || props.getMerge() == null || props.getMerge() != 1)
            throw new IllegalArgumentException("该物品不能作为定情信物");

        int current = gift.getSums() != null ? gift.getSums() : 0;
        if (current < count) throw new IllegalArgumentException("物品数量不足");

        // Consume gift and record in send field
        if (current <= count) bagRepo.delete(gift);
        else { gift.setSums(current - count); bagRepo.save(gift); }

        myExt.setRequestMerge(targetPlayerId.intValue());
        myExt.setRequest(0);
        myExt.setSend(count + "," + gift.getPropId());
        extRepo.save(myExt);

        targetExt.setRequestMerge(playerId);
        targetExt.setRequest(0);
        extRepo.save(targetExt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("proposed", target.getNickname());
        result.put("gift", props.getName());
        return result;
    }

    /** Accept marriage proposal */
    @Transactional
    public Map<String, Object> acceptMarriage(Integer playerId, Long proposerId) {
        PlayerExt myExt = getOrCreateExt(playerId);
        PlayerExt proposerExt = extRepo.findById(proposerId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("求婚数据异常"));

        if (proposerExt.getRequestMerge() == null || !proposerExt.getRequestMerge().equals(playerId))
            throw new IllegalArgumentException("对方没有向你求婚");

        if (myExt.getMerge() != null && myExt.getMerge() > 0)
            throw new IllegalArgumentException("你已结婚");

        // Give the gift to the acceptor
        String send = proposerExt.getSend();
        if (send != null && !send.isEmpty()) {
            String[] parts = send.split(",");
            if (parts.length >= 2) {
                int giftCount = Integer.parseInt(parts[0]);
                long giftPropId = Long.parseLong(parts[1]);
                giveItem(playerId, giftPropId, giftCount);
            }
        }

        // Set both as married
        myExt.setMerge(proposerId.intValue());
        myExt.setRequestMerge(0);
        myExt.setSend("0");
        extRepo.save(myExt);

        proposerExt.setMerge(playerId);
        proposerExt.setRequestMerge(0);
        proposerExt.setRequest(0);
        proposerExt.setSend("0");
        extRepo.save(proposerExt);

        Player proposer = playerRepo.findById(proposerId.intValue()).orElse(null);
        Player me = playerRepo.findById(playerId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("married", true);
        result.put("spouse", proposer != null ? proposer.getNickname() : "");
        result.put("announce", (me != null ? me.getNickname() : "") + " 和 " + (proposer != null ? proposer.getNickname() : "") + " 结为夫妻！");
        return result;
    }

    /** Request divorce: costs 2000 crystals, 24h cooling */
    @Transactional
    public Map<String, Object> requestDivorce(Integer playerId) {
        PlayerExt myExt = getOrCreateExt(playerId);
        if (myExt.getMerge() == null || myExt.getMerge() == 0)
            throw new IllegalArgumentException("你未结婚");

        if (myExt.getRequest() != null && myExt.getRequest() == 1)
            throw new IllegalArgumentException("已提出离婚，等待对方响应");

        int sj = myExt.getSj() != null ? myExt.getSj() : 0;
        if (sj < DIVORCE_COST)
            throw new IllegalArgumentException("水晶不足，需要" + DIVORCE_COST + "水晶");

        myExt.setSj(sj - DIVORCE_COST);
        myExt.setRequest(1);
        myExt.setNomergetime((int)(System.currentTimeMillis() / 1000));
        extRepo.save(myExt);

        // Notify spouse
        PlayerExt spouseExt = getOrCreateExt(myExt.getMerge());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("divorceRequested", true);
        result.put("cost", DIVORCE_COST);
        result.put("message", "离婚请求已发送，24小时内对方可响应");
        return result;
    }

    /** Accept divorce from spouse */
    @Transactional
    public Map<String, Object> acceptDivorce(Integer playerId) {
        PlayerExt myExt = getOrCreateExt(playerId);
        if (myExt.getMerge() == null || myExt.getMerge() == 0)
            throw new IllegalArgumentException("你未结婚");

        PlayerExt spouseExt = extRepo.findById(myExt.getMerge()).orElse(null);
        if (spouseExt == null || spouseExt.getRequest() == null || spouseExt.getRequest() != 1)
            throw new IllegalArgumentException("对方未提出离婚");

        // Divorce
        myExt.setMerge(0);
        myExt.setRequest(0);
        extRepo.save(myExt);

        spouseExt.setMerge(0);
        spouseExt.setRequest(0);
        extRepo.save(spouseExt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("divorced", true);
        return result;
    }

    /** Cancel own divorce request (refunds crystals) */
    @Transactional
    public Map<String, Object> cancelDivorce(Integer playerId) {
        PlayerExt myExt = getOrCreateExt(playerId);
        if (myExt.getRequest() == null || myExt.getRequest() != 1)
            throw new IllegalArgumentException("未提出离婚");

        myExt.setSj((myExt.getSj() != null ? myExt.getSj() : 0) + DIVORCE_COST);
        myExt.setRequest(0);
        extRepo.save(myExt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cancelled", true);
        result.put("refunded", DIVORCE_COST + " 水晶");
        return result;
    }

    /** Get marriage status */
    public Map<String, Object> getMarriageStatus(Integer playerId) {
        PlayerExt ext = getOrCreateExt(playerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("married", ext.getMerge() != null && ext.getMerge() > 0);
        if (ext.getMerge() != null && ext.getMerge() > 0) {
            Player spouse = playerRepo.findById(ext.getMerge()).orElse(null);
            result.put("spouseId", ext.getMerge());
            result.put("spouseName", spouse != null ? spouse.getNickname() : "未知");
        }
        result.put("pendingProposal", ext.getRequestMerge() != null && ext.getRequestMerge() > 0);
        result.put("divorceRequested", ext.getRequest() != null && ext.getRequest() == 1);
        result.put("crystals", ext.getSj());
        return result;
    }

    /** Check and clean up expired divorce requests */
    public void checkDivorceTimeout(Integer playerId) {
        PlayerExt ext = extRepo.findById(playerId).orElse(null);
        if (ext != null && ext.getRequest() != null && ext.getRequest() == 1) {
            int now = (int)(System.currentTimeMillis() / 1000);
            if (ext.getNomergetime() != null && (now - ext.getNomergetime()) >= DIVORCE_COOLDOWN) {
                // Auto-cancel: refund crystals
                ext.setSj((ext.getSj() != null ? ext.getSj() : 0) + DIVORCE_COST);
                ext.setRequest(0);
                extRepo.save(ext);
            }
        }
    }

    private void giveItem(Integer playerId, long propId, int count) {
        Props props = propsRepo.findById(propId).orElse(null);
        UserBag existing = bagRepo.findByPlayerId(playerId.longValue()).stream()
            .filter(b -> b.getPropId() != null && b.getPropId() == propId)
            .findFirst().orElse(null);
        if (existing != null && (props == null || props.getVary() == null || props.getVary() == 1)) {
            existing.setSums((existing.getSums() != null ? existing.getSums() : 0) + count);
            bagRepo.save(existing);
        } else {
            UserBag newItem = new UserBag();
            newItem.setPlayerId(playerId.longValue());
            newItem.setPropId(propId);
            newItem.setSums(count);
            newItem.setVary(props != null ? props.getVary() : 1);
            newItem.setSell(props != null ? props.getSell() : 0);
            newItem.setZbing(0);
            newItem.setPyb(0); newItem.setPsell(0); newItem.setPstime(0L);
            newItem.setBsum(0); newItem.setPetime(0L); newItem.setPsum(0);
            newItem.setStime(System.currentTimeMillis() / 1000);
            bagRepo.save(newItem);
        }
    }
}
