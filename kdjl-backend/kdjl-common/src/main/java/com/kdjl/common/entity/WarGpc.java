package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_gpc")
public class WarGpc {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50) private String name;
    @Column(name = "drop_list", length = 100) private String dropList;
    @Column(length = 100) private String imgack;
    @Column(length = 100) private String img;
    @Column(name = "talent_id", length = 20) private String talentId;
    @Column private Integer level;
    @Column private Integer speed;
    @Column private Integer hp;
    @Column private Integer attack;
    @Column private Integer defense;
    @Column private Integer wuxing;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDropList() { return dropList; }
    public String getImgack() { return imgack; }
    public String getImg() { return img; }
    public String getTalentId() { return talentId; }
    public Integer getLevel() { return level; }
    public Integer getSpeed() { return speed; }
    public Integer getHp() { return hp; }
    public Integer getAttack() { return attack; }
    public Integer getDefense() { return defense; }
    public Integer getWuxing() { return wuxing; }
}
