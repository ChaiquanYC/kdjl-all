package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "merge")
public class Merge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Integer aid;

    @Column
    private Integer bid;

    @Column
    private Integer maid;

    @Column
    private Integer mbid;

    @Column(length = 20)
    private String limits;

    public Long getId() { return id; }
    public Integer getAid() { return aid; }
    public Integer getBid() { return bid; }
    public Integer getMaid() { return maid; }
    public Integer getMbid() { return mbid; }
    public String getLimits() { return limits; }
}
