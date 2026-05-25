package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_invite")
public class WarInvite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id") private Long teamId;
    @Column(name = "uid") private Long playerId;
    @Column(name = "`type`") private Integer type;
    @Column private Integer state;

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public Long getPlayerId() { return playerId; }
    public Integer getType() { return type; }
    public Integer getState() { return state; }
}
