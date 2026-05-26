package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "super_jh")
public class SuperJh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pet_id")
    private Integer petId;

    @Column(name = "need_levels", length = 120)
    private String needLevels;

    @Column(name = "need_props", length = 255)
    private String needProps;

    @Column(name = "max_czl")
    private Integer maxCzl;

    @Column(name = "zs_progress")
    private Integer zsProgress;

    @Column(name = "zs_line", length = 10)
    private String zsLine;

    @Column(name = "max_level")
    private Integer maxLevel;

    public Integer getId() { return id; }
    public Integer getPetId() { return petId; }
    public String getNeedLevels() { return needLevels; }
    public String getNeedProps() { return needProps; }
    public Integer getMaxCzl() { return maxCzl; }
    public Integer getZsProgress() { return zsProgress; }
    public String getZsLine() { return zsLine; }
    public Integer getMaxLevel() { return maxLevel; }
}
