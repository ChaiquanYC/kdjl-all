package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "zs")
public class Zs {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column private Integer aid;
    @Column private Integer bid;
    @Column private Integer mid;
    public Long getId() { return id; }
    public Integer getAid() { return aid; }
    public Integer getBid() { return bid; }
    public Integer getMid() { return mid; }
}
