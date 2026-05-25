package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "props")
public class Props {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String requires;

    @Column(length = 255)
    private String usages;

    @Column(length = 1000)
    private String effect;

    @Column
    private Integer sell;

    @Column
    private Integer prestige;

    @Column
    private Integer buy;

    @Column
    private Integer yb; // ingot price

    @Column
    private Integer sj;

    @Column
    private Long stime;

    @Column
    private Long endtime;

    @Column(length = 50)
    private String img;

    @Column
    private Integer vary;

    @Column
    private Integer merge; // 1 = marriage proposal gift

    @Column
    private Integer varyname;

    @Column
    private Integer postion;

    @Column(length = 100)
    private String pluseffect;

    @Column
    private Integer plusflag;

    @Column(name = "pluspid")
    private Long plusPropId;

    @Column(length = 255)
    private String plusget;

    @Column
    private Integer plusnum;

    @Column(length = 7)
    private String propscolor;

    @Column
    private Integer propslock;

    @Column(length = 255)
    private String series;

    @Column(length = 255)
    private String serieseffect;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRequires() { return requires; }
    public String getUsages() { return usages; }
    public String getEffect() { return effect; }
    public Integer getSell() { return sell; }
    public Integer getPrestige() { return prestige; }
    public Integer getBuy() { return buy; }
    public Integer getYb() { return yb; }
    public Integer getSj() { return sj; }
    public Long getStime() { return stime; }
    public Long getEndtime() { return endtime; }
    public String getImg() { return img; }
    public Integer getVary() { return vary; }
    public Integer getMerge() { return merge; }
    public Integer getVaryname() { return varyname; }
    public Integer getPostion() { return postion; }
    public String getPluseffect() { return pluseffect; }
    public Integer getPlusflag() { return plusflag; }
    public Long getPlusPropId() { return plusPropId; }
    public String getPlusget() { return plusget; }
    public Integer getPlusnum() { return plusnum; }
    public String getPropscolor() { return propscolor; }
    public Integer getPropslock() { return propslock; }
    public String getSeries() { return series; }
    public String getSerieseffect() { return serieseffect; }
}
