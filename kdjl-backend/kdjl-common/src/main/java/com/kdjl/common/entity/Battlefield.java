package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "battlefield")
public class Battlefield {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String posname;

    @Column private Long srchp;
    @Column private Long hp;
    @Column private Integer maxuser;

    @Column(name = "level_get", length = 255)
    private String levelGet;

    @Column private Long bfdate;
    @Column(name = "start_time") private Long startTime;
    @Column(name = "end_time") private Long endTime;
    @Column(name = "tips_time") private Long tipsTime;
    @Column(name = "bf_ml_num") private Integer bfMlNum;
    @Column(name = "bf_level_limit") private Integer bfLevelLimit;

    @Column private Integer countf;
    @Column private Integer startf;
    @Column private Integer success;
    @Column private Integer ends;

    public Long getId() { return id; }
    public String getPosname() { return posname; }
    public Long getSrchp() { return srchp; }
    public Long getHp() { return hp; }
    public Integer getMaxuser() { return maxuser; }
    public String getLevelGet() { return levelGet; }
    public Long getBfdate() { return bfdate; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public Long getTipsTime() { return tipsTime; }
    public Integer getBfMlNum() { return bfMlNum; }
    public Integer getBfLevelLimit() { return bfLevelLimit; }
    public Integer getCountf() { return countf; }
    public Integer getStartf() { return startf; }
    public Integer getSuccess() { return success; }
    public Integer getEnds() { return ends; }
}
