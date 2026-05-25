package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "boss_refresh")
public class BossRefresh {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gid") private Long monsterId;
    @Column private Long rtime;
    @Column(name = "fightuid") private Long fightPlayerId;
    @Column private Long dtime;
    @Column private Integer glock;

    public Long getId() { return id; }
    public Long getMonsterId() { return monsterId; }
    public Long getRtime() { return rtime; }
    public Long getFightPlayerId() { return fightPlayerId; }
    public Long getDtime() { return dtime; }
    public Integer getGlock() { return glock; }
}
