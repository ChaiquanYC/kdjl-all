package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepo;
    private final TeamMembersRepository memberRepo;
    private final PlayerRepository playerRepo;
    private static final int MAX_MEMBERS = 4;

    public TeamService(TeamRepository teamRepo, TeamMembersRepository memberRepo, PlayerRepository playerRepo) {
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
        this.playerRepo = playerRepo;
    }

    public List<Map<String, Object>> listTeams() {
        return teamRepo.findAll().stream().map(t -> {
            long cnt = memberRepo.countByTeamId(t.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId()); m.put("name", t.getName());
            m.put("creatorId", t.getCreator()); m.put("memberCount", cnt);
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTeam(Integer playerId, String name) {
        if (memberRepo.findById(new TeamMembers.TeamMembersId(null, playerId.longValue())).isPresent())
            throw new IllegalArgumentException("你已在队伍中");
        Team t = new Team();
        t.setName(name != null ? name : "队伍");
        t.setCreator(playerId.longValue());
        t.setCreateTime(System.currentTimeMillis() / 1000);
        t = teamRepo.save(t);
        TeamMembers m = new TeamMembers();
        m.setTeamId(t.getId()); m.setPlayerId(playerId.longValue()); m.setState(1);
        m.setApplyTime(System.currentTimeMillis() / 1000);
        memberRepo.save(m);
        return Map.of("created", true, "teamId", t.getId());
    }

    @Transactional
    public Map<String, Object> joinTeam(Integer playerId, Long teamId) {
        if (memberRepo.findById(new TeamMembers.TeamMembersId(null, playerId.longValue())).isPresent())
            throw new IllegalArgumentException("你已在队伍中");
        teamRepo.findById(teamId).orElseThrow(() -> new IllegalArgumentException("队伍不存在"));
        if (memberRepo.countByTeamId(teamId) >= MAX_MEMBERS)
            throw new IllegalArgumentException("队伍已满");
        TeamMembers m = new TeamMembers();
        m.setTeamId(teamId); m.setPlayerId(playerId.longValue()); m.setState(1);
        m.setApplyTime(System.currentTimeMillis() / 1000);
        memberRepo.save(m);
        return Map.of("joined", true);
    }

    @Transactional
    public Map<String, Object> leaveTeam(Integer playerId) {
        var id = new TeamMembers.TeamMembersId(null, playerId.longValue());
        TeamMembers m = memberRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("你不在队伍中"));
        memberRepo.delete(m);
        if (memberRepo.countByTeamId(m.getTeamId()) == 0) teamRepo.deleteById(m.getTeamId());
        return Map.of("left", true);
    }

    public Map<String, Object> getMyTeam(Integer playerId) {
        var teams = memberRepo.findByPlayerId(playerId.longValue());
        if (teams.isEmpty()) return null;
        TeamMembers m = teams.get(0);
        Team t = teamRepo.findById(m.getTeamId()).orElse(null);
        if (t == null) return null;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", t.getId()); r.put("name", t.getName()); r.put("creatorId", t.getCreator());
        List<TeamMembers> members = memberRepo.findByTeamId(t.getId());
        r.put("members", members.stream().map(tm -> {
            Player p = playerRepo.findById(tm.getPlayerId().intValue()).orElse(null);
            return Map.of("playerId", tm.getPlayerId(), "nickname", p != null ? p.getNickname() : "未知", "state", tm.getState());
        }).collect(Collectors.toList()));
        return r;
    }

    /** Kick a member — leader only. PHP: team.v1.php kickMember() */
    @Transactional
    public Map<String, Object> kickMember(Integer leaderId, Long targetPlayerId) {
        var leaderTeams = memberRepo.findByPlayerId(leaderId.longValue());
        if (leaderTeams.isEmpty()) return Map.of("error", "你没有加入队伍！");
        TeamMembers leader = leaderTeams.get(0);
        Team t = teamRepo.findById(leader.getTeamId()).orElse(null);
        if (t == null) return Map.of("error", "队伍信息丢失!");
        if (!t.getCreator().equals(leaderId.longValue()))
            return Map.of("error", "只有队长才能踢人！");
        if (leaderId.longValue() == targetPlayerId)
            return Map.of("error", "不能踢自己！");

        var id = new TeamMembers.TeamMembersId(leader.getTeamId(), targetPlayerId);
        TeamMembers target = memberRepo.findById(id).orElse(null);
        if (target == null) return Map.of("error", "队伍中无此成员!");
        memberRepo.delete(target);
        return Map.of("kicked", true);
    }

    /** Approve a pending member — leader only. State: -1→1 */
    @Transactional
    public Map<String, Object> approveMember(Integer leaderId, Long targetPlayerId) {
        var leaderTeams = memberRepo.findByPlayerId(leaderId.longValue());
        if (leaderTeams.isEmpty()) return Map.of("error", "你没有加入队伍！");
        TeamMembers leader = leaderTeams.get(0);
        Team t = teamRepo.findById(leader.getTeamId()).orElse(null);
        if (t == null) return Map.of("error", "队伍信息丢失!");
        if (!t.getCreator().equals(leaderId.longValue()))
            return Map.of("error", "只有队长才能审批！");

        var id = new TeamMembers.TeamMembersId(leader.getTeamId(), targetPlayerId);
        TeamMembers target = memberRepo.findById(id).orElse(null);
        if (target == null) return Map.of("error", "找不到该成员!");
        if (target.getState() == null || target.getState() != -1)
            return Map.of("error", "该成员不在待审批状态!");
        target.setState(1);
        memberRepo.save(target);
        return Map.of("approved", true);
    }

    /** Toggle away status — state 1→0 or 0→1. PHP: team.v1.php */
    @Transactional
    public Map<String, Object> toggleAway(Integer playerId) {
        var teams = memberRepo.findByPlayerId(playerId.longValue());
        if (teams.isEmpty()) return Map.of("error", "你不在队伍中！");
        TeamMembers m = teams.get(0);
        int currentState = m.getState() != null ? m.getState() : 1;
        // Don't allow leader to go away (PHP check)
        Team t = teamRepo.findById(m.getTeamId()).orElse(null);
        if (t != null && t.getCreator().equals(playerId.longValue()))
            return Map.of("error", "队长不能暂离！");
        int newState = currentState == 1 ? 0 : 1;
        m.setState(newState);
        memberRepo.save(m);
        return Map.of("away", newState == 0);
    }
}
