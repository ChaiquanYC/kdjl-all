package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gpc")
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column
    private Integer level;

    @Column
    private Long hp;

    @Column
    private Long mp;

    @Column
    private Long ac; // physical atk

    @Column
    private Long mc; // magic atk

    @Column
    private Long speed;

    @Column
    private Long hits;

    @Column
    private Long miss;

    @Column
    private Integer catchv; // catch probability

    @Column(name = "catchid")
    private Long catchItemId;

    @Column(length = 50)
    private String skill;

    @Column(length = 50)
    private String imgstand;

    @Column(length = 50)
    private String imgack;

    @Column(length = 50)
    private String imgdie;

    @Column(length = 255)
    private String droplist;

    @Column
    private Long exps;

    @Column
    private Long money;

    @Column
    private Integer boss; // 1 = boss

    @Column
    private Integer wx; // element

    @Column(length = 255, nullable = false)
    private String kx; // resistances

    @Column(name = "activedroplist", length = 30, nullable = false)
    private String activeDropList;

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public Long getHp() { return hp; }
    public Long getMp() { return mp; }
    public Long getAc() { return ac; }
    public Long getMc() { return mc; }
    public Long getSpeed() { return speed; }
    public Long getHits() { return hits; }
    public Long getMiss() { return miss; }
    public Integer getCatchv() { return catchv; }
    public Long getCatchItemId() { return catchItemId; }
    public String getSkill() { return skill; }
    public String getImgstand() { return imgstand; }
    public String getImgack() { return imgack; }
    public String getImgdie() { return imgdie; }
    public String getDroplist() { return droplist; }
    public Long getExps() { return exps; }
    public Long getMoney() { return money; }
    public Integer getBoss() { return boss; }
    public Integer getWx() { return wx; }
    public String getKx() { return kx; }
    public String getActiveDropList() { return activeDropList; }
}
