package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_default_talent")
public class WarDefaultTalent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pid") private Integer pid;
    @Column(name = "tid") private Integer tid;
    public Long getId() { return id; }
    public Integer getPid() { return pid; }
    public Integer getTid() { return tid; }
}
