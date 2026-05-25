package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "battlelog")
public class Battlelog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column private Long jgvalue;
    @Column private Long curjgvalue;
    @Column private Long jgtime;
    @Column private Long sumjg;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getJgvalue() { return jgvalue; }
    public Long getCurjgvalue() { return curjgvalue; }
    public Long getJgtime() { return jgtime; }
    public Long getSumjg() { return sumjg; }
}
