package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fight_log")
public class FightLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column private Long time;
    @Column private Integer vary;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getTime() { return time; }
    public Integer getVary() { return vary; }
}
