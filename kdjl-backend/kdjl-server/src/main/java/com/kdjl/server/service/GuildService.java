package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuildService {

    private final GuildRepository guildRepo;
    private final GuildMembersRepository memberRepo;
    private final PlayerRepository playerRepo;

    private static final int CREATE_COST = 10000;
    private static final int MAX_MEMBERS = 50;

    public GuildService(GuildRepository guildRepo, GuildMembersRepository memberRepo,
                        PlayerRepository playerRepo) {
        this.guildRepo = guildRepo;
        this.memberRepo = memberRepo;
        this.playerRepo = playerRepo;
    }

    /** List all guilds */
    public List<Map<String, Object>> listGuilds() {
        return guildRepo.findAll().stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("level", g.getLevel());
            m.put("memberCount", g.getMemberCount());
            m.put("honor", g.getHonor());
            m.put("info", g.getInfo());
            return m;
        }).collect(Collectors.toList());
    }

    /** Get guild detail with members */
    public Map<String, Object> getGuild(Long guildId) {
        Guild g = guildRepo.findById(guildId)
            .orElseThrow(() -> new IllegalArgumentException("公会不存在"));
        List<GuildMembers> members = memberRepo.findByGuildId(guildId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());
        m.put("info", g.getInfo());
        m.put("level", g.getLevel());
        m.put("honor", g.getHonor());
        m.put("memberCount", g.getMemberCount());
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (GuildMembers gm : members) {
            Player p = playerRepo.findById(gm.getMemberId().intValue()).orElse(null);
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("playerId", gm.getMemberId());
            mm.put("nickname", p != null ? p.getNickname() : "未知");
            mm.put("priv", gm.getPriv());
            mm.put("contribution", gm.getContribution());
            memberList.add(mm);
        }
        m.put("members", memberList);
        return m;
    }

    /** Create guild */
    @Transactional
    public Map<String, Object> createGuild(Integer playerId, String name, String info) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("请输入公会名称");

        Player player = playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        // Check money
        int money = player.getMoney() != null ? player.getMoney() : 0;
        if (money < CREATE_COST)
            throw new IllegalArgumentException("金币不足，需要" + CREATE_COST + "金币");

        // Check not already in guild
        if (memberRepo.findByMemberId(playerId.longValue()).isPresent())
            throw new IllegalArgumentException("你已加入公会，请先退出");

        // Check name unique
        if (guildRepo.findByName(name).isPresent())
            throw new IllegalArgumentException("公会名称已存在");

        player.setMoney(money - CREATE_COST);
        playerRepo.save(player);

        Guild guild = new Guild();
        guild.setName(name);
        guild.setInfo(info != null ? info : "");
        guild.setCreatorId(String.valueOf(playerId));
        guild.setPresidentId(playerId.longValue());
        guild.setLevel(1);
        guild.setShopLevel(1);
        guild.setMemberCount(1);
        guild.setHonor(0);
        guild.setCreateTime(System.currentTimeMillis() / 1000);
        guild = guildRepo.save(guild);

        GuildMembers gm = new GuildMembers();
        gm.setMemberId(playerId.longValue());
        gm.setGuildId(guild.getId());
        gm.setPriv(3); // master
        gm.setContribution(0);
        gm.setJoinTime(System.currentTimeMillis() / 1000);
        memberRepo.save(gm);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", true);
        result.put("guildId", guild.getId());
        result.put("guildName", guild.getName());
        return result;
    }

    /** Join guild */
    @Transactional
    public Map<String, Object> joinGuild(Integer playerId, Long guildId) {
        Guild g = guildRepo.findById(guildId)
            .orElseThrow(() -> new IllegalArgumentException("公会不存在"));
        if (g.getMemberCount() != null && g.getMemberCount() >= MAX_MEMBERS)
            throw new IllegalArgumentException("公会已满");
        if (memberRepo.findByMemberId(playerId.longValue()).isPresent())
            throw new IllegalArgumentException("你已加入公会");

        GuildMembers gm = new GuildMembers();
        gm.setMemberId(playerId.longValue());
        gm.setGuildId(guildId);
        gm.setPriv(1);
        gm.setContribution(0);
        gm.setJoinTime(System.currentTimeMillis() / 1000);
        memberRepo.save(gm);

        g.setMemberCount((g.getMemberCount() != null ? g.getMemberCount() : 0) + 1);
        guildRepo.save(g);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("joined", g.getName());
        return result;
    }

    /** Leave guild */
    @Transactional
    public Map<String, Object> leaveGuild(Integer playerId) {
        GuildMembers gm = memberRepo.findByMemberId(playerId.longValue())
            .orElseThrow(() -> new IllegalArgumentException("你未加入公会"));

        Guild g = guildRepo.findById(gm.getGuildId()).orElse(null);
        if (g != null && g.getPresidentId() != null && g.getPresidentId().equals(playerId.longValue())) {
            // Master leaving = disband
            List<GuildMembers> members = memberRepo.findByGuildId(g.getId());
            memberRepo.deleteAll(members);
            guildRepo.delete(g);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("disbanded", true);
            return result;
        }

        memberRepo.delete(gm);
        if (g != null) {
            g.setMemberCount(Math.max(0, (g.getMemberCount() != null ? g.getMemberCount() : 1) - 1));
            guildRepo.save(g);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("left", true);
        return result;
    }

    /** Get my guild info */
    public Map<String, Object> getMyGuild(Integer playerId) {
        GuildMembers gm = memberRepo.findByMemberId(playerId.longValue()).orElse(null);
        if (gm == null) return null;
        return getGuild(gm.getGuildId());
    }
}
