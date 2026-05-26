package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "super_zs")
public class SuperZs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cur_pet_id")
    private Integer curPetId;

    @Column(name = "need_level")
    private Integer needLevel;

    @Column(name = "need_czl")
    private Integer needCzl;

    @Column(name = "need_props", length = 100)
    private String needProps;

    @Column(name = "base_success_rate")
    private Integer baseSuccessRate;

    @Column(name = "failed_czl_percent")
    private Integer failedCzlPercent;

    @Column(name = "next_pet_id", length = 255)
    private String nextPetId;

    public Integer getId() { return id; }
    public Integer getCurPetId() { return curPetId; }
    public Integer getNeedLevel() { return needLevel; }
    public Integer getNeedCzl() { return needCzl; }
    public String getNeedProps() { return needProps; }
    public Integer getBaseSuccessRate() { return baseSuccessRate; }
    public Integer getFailedCzlPercent() { return failedCzlPercent; }
    public String getNextPetId() { return nextPetId; }
}
