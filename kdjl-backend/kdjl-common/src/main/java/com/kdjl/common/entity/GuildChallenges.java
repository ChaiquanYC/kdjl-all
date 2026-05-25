package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guild_challenges")
public class GuildChallenges {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "challenger_id") private Long challengerId;
    @Column(name = "defenser_id") private Long defenderId;
    @Column(name = "challenge_msg", length = 255) private String challengeMsg;
    @Column(name = "create_time") private Long createTime;
    @Column(name = "challenger_score") private Integer challengerScore;
    @Column(name = "defenser_score") private Integer defenderScore;
    @Column private Integer flags;
    public Long getId() { return id; }
    public Long getChallengerId() { return challengerId; }
    public Long getDefenderId() { return defenderId; }
    public String getChallengeMsg() { return challengeMsg; }
    public Long getCreateTime() { return createTime; }
    public Integer getChallengerScore() { return challengerScore; }
    public Integer getDefenderScore() { return defenderScore; }
    public Integer getFlags() { return flags; }
}
