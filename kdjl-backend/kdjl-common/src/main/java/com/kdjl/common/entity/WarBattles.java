package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_battles")
public class WarBattles {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`type`") private Integer type;
    @Column(name = "next_id", length = 50) private String nextId;
    @Column(length = 100) private String monsters;
    @Column(name = "monsters_num_min") private Integer monstersNumMin;
    @Column(name = "monsters_num_max") private Integer monstersNumMax;
    @Column(name = "block_level") private Integer blockLevel;
    @Column(length = 50) private String descs;
    @Column private Integer wuxing;
    @Column(name = "extra_prize", length = 50) private String extraPrize;
    @Column(length = 50) private String img;
    @Column(name = "special_img", length = 50) private String specialImg;
    @Column(name = "buff_effect", length = 80) private String buffEffect;

    public Long getId() { return id; }
    public Integer getType() { return type; }
    public String getNextId() { return nextId; }
    public String getMonsters() { return monsters; }
    public Integer getMonstersNumMin() { return monstersNumMin; }
    public Integer getMonstersNumMax() { return monstersNumMax; }
    public Integer getBlockLevel() { return blockLevel; }
    public String getDescs() { return descs; }
    public Integer getWuxing() { return wuxing; }
    public String getExtraPrize() { return extraPrize; }
    public String getImg() { return img; }
    public String getSpecialImg() { return specialImg; }
    public String getBuffEffect() { return buffEffect; }
}
