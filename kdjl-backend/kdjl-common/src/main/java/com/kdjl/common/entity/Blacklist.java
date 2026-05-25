package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "blacklist")
public class Blacklist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(columnDefinition = "text") private String list;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getList() { return list; }
}
