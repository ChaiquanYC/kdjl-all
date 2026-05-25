package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guild_members")
public class GuildMembers {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "guild_id") private Long guildId;
    @Column(name = "join_time") private Long joinTime;
    @Column private Integer priv; // 1=member, 2=elder, 3=master
    @Column private Integer contribution;
    @Column private Integer honor;

    public Long getMemberId() { return memberId; }
    public Long getGuildId() { return guildId; }
    public Long getJoinTime() { return joinTime; }
    public Integer getPriv() { return priv; }
    public Integer getContribution() { return contribution; }
    public Integer getHonor() { return honor; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public void setGuildId(Long guildId) { this.guildId = guildId; }
    public void setJoinTime(Long joinTime) { this.joinTime = joinTime; }
    public void setPriv(Integer priv) { this.priv = priv; }
    public void setContribution(Integer contribution) { this.contribution = contribution; }
    public void setHonor(Integer honor) { this.honor = honor; }
}
