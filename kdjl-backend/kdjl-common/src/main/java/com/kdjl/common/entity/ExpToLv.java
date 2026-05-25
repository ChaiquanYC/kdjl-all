package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "exptolv")
public class ExpToLv {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column private Integer level;
    @Column(name = "nxtlvexp") private Long nextLevelExp;
    public Long getId() { return id; }
    public Integer getLevel() { return level; }
    public Long getNextLevelExp() { return nextLevelExp; }
}
