package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tgt")
public class Tgt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column(name = "gid") private Long monsterId;
    @Column private Long boss;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getMonsterId() { return monsterId; }
    public Long getBoss() { return boss; }
}
