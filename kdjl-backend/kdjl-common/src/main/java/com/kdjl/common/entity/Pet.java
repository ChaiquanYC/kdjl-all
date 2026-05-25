package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bb")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column
    private Integer wx; // element: 金1木2水3火4土5

    @Column
    private Long ac;

    @Column
    private Long mc;

    @Column
    private Long hp;

    @Column
    private Long mp;

    @Column
    private Integer speed;

    @Column
    private Integer hits;

    @Column
    private Integer miss;

    @Column(length = 50)
    private String imgstand;

    @Column(length = 50)
    private String imgack;

    @Column(length = 50)
    private String imgdie;

    @Column(name = "skillist", length = 255)
    private String skillList;

    @Column(length = 50)
    private String czl; // growth rating string

    @Column(length = 255)
    private String kx; // element resistances

    @Column(name = "remakelevel", length = 30)
    private String remakeLevel;

    @Column(name = "remakeid", length = 30)
    private String remakeId;

    @Column(name = "remakepid", length = 30)
    private String remakePid;

    @Column
    private Long nowexp;

    @Column
    private Long lexp;

    @Column
    private Integer subyl; // gold growth
    @Column
    private Integer subsl; // wood growth
    @Column
    private Integer subxl; // water growth
    @Column
    private Integer subdl; // fire growth
    @Column
    private Integer subfl; // earth growth
    @Column
    private Integer subhl; // hp growth
    @Column
    private Integer subkl; // speed growth

    @Column(length = 50)
    private String headimg;

    @Column(length = 50)
    private String cardimg;

    @Column(length = 50)
    private String effectimg;

    // Below columns don't exist in the actual bb table schema — kept as @Transient
    @Transient
    private Integer evoLevel;

    @Transient
    private Long evoId;

    @Transient
    private Long evoPid;

    @Transient
    private Integer quality;

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getWx() { return wx; }
    public Long getAc() { return ac; }
    public Long getMc() { return mc; }
    public Long getHp() { return hp; }
    public Long getMp() { return mp; }
    public Integer getSpeed() { return speed; }
    public Integer getHits() { return hits; }
    public Integer getMiss() { return miss; }
    public String getImgstand() { return imgstand; }
    public String getImgack() { return imgack; }
    public String getImgdie() { return imgdie; }
    public String getSkillList() { return skillList; }
    public String getCzl() { return czl; }
    public String getKx() { return kx; }
    public String getRemakeLevel() { return remakeLevel; }
    public String getRemakeId() { return remakeId; }
    public String getRemakePid() { return remakePid; }
    public Long getNowexp() { return nowexp; }
    public Long getLexp() { return lexp; }
    public Integer getSubyl() { return subyl; }
    public Integer getSubsl() { return subsl; }
    public Integer getSubxl() { return subxl; }
    public Integer getSubdl() { return subdl; }
    public Integer getSubfl() { return subfl; }
    public Integer getSubhl() { return subhl; }
    public Integer getSubkl() { return subkl; }
    public String getHeadimg() { return headimg; }
    public String getCardimg() { return cardimg; }
    public String getEffectimg() { return effectimg; }
    public Integer getEvoLevel() { return evoLevel; }
    public Long getEvoId() { return evoId; }
    public Long getEvoPid() { return evoPid; }
    public Integer getQuality() { return quality; }
}
