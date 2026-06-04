package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_Card_to_Title")
public class CardToTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "F_title_name", length = 30)
    private String titleName;

    @Column(name = "F_title_Chinese", length = 30)
    private String titleChinese;

    @Column(name = "F_title_img", length = 70)
    private String titleImg;

    @Column(name = "F_title_must_card", columnDefinition = "TEXT")
    private String mustCard;

    @Column(name = "F_title_get_methods", columnDefinition = "TEXT")
    private String getMethods;

    @Column(name = "F_add_hp")
    private Integer addHp;

    @Column(name = "F_add_mp")
    private Integer addMp;

    @Column(name = "F_add_ac")
    private Integer addAc;

    @Column(name = "F_add_mc")
    private Integer addMc;

    @Column(name = "F_add_hits")
    private Integer addHits;

    @Column(name = "F_add_miss")
    private Integer addMiss;

    @Column(name = "F_add_speed")
    private Integer addSpeed;

    @Column(name = "F_add_hprate")
    private Integer addHprate;

    @Column(name = "F_add_mprate")
    private Integer addMprate;

    @Column(name = "F_add_acrate")
    private Integer addAcrate;

    @Column(name = "F_add_mcrate")
    private Integer addMcrate;

    @Column(name = "F_add_hitsrate")
    private Integer addHitsrate;

    @Column(name = "F_add_missrate")
    private Integer addMissrate;

    @Column(name = "F_add_speedrate")
    private Integer addSpeedrate;

    @Column(name = "F_dxsh")
    private Integer dxsh;

    @Column(name = "F_hitshp")
    private Integer hitshp;

    @Column(name = "F_hitsmp")
    private Integer hitsmp;

    @Column(name = "F_shjs")
    private Integer shjs;

    @Column(name = "F_sdmp")
    private Integer sdmp;

    @Column(name = "F_szmp")
    private Integer szmp;

    @Column(name = "F_addmoney")
    private Integer addmoney;

    @Column(name = "F_time")
    private Integer time;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitleName() { return titleName; }
    public void setTitleName(String titleName) { this.titleName = titleName; }
    public String getTitleChinese() { return titleChinese; }
    public void setTitleChinese(String titleChinese) { this.titleChinese = titleChinese; }
    public String getTitleImg() { return titleImg; }
    public void setTitleImg(String titleImg) { this.titleImg = titleImg; }
    public String getMustCard() { return mustCard; }
    public void setMustCard(String mustCard) { this.mustCard = mustCard; }
    public String getGetMethods() { return getMethods; }
    public void setGetMethods(String getMethods) { this.getMethods = getMethods; }
    public Integer getAddHp() { return addHp; }
    public void setAddHp(Integer addHp) { this.addHp = addHp; }
    public Integer getAddMp() { return addMp; }
    public void setAddMp(Integer addMp) { this.addMp = addMp; }
    public Integer getAddAc() { return addAc; }
    public void setAddAc(Integer addAc) { this.addAc = addAc; }
    public Integer getAddMc() { return addMc; }
    public void setAddMc(Integer addMc) { this.addMc = addMc; }
    public Integer getAddHits() { return addHits; }
    public void setAddHits(Integer addHits) { this.addHits = addHits; }
    public Integer getAddMiss() { return addMiss; }
    public void setAddMiss(Integer addMiss) { this.addMiss = addMiss; }
    public Integer getAddSpeed() { return addSpeed; }
    public void setAddSpeed(Integer addSpeed) { this.addSpeed = addSpeed; }
    public Integer getAddHprate() { return addHprate; }
    public void setAddHprate(Integer addHprate) { this.addHprate = addHprate; }
    public Integer getAddMprate() { return addMprate; }
    public void setAddMprate(Integer addMprate) { this.addMprate = addMprate; }
    public Integer getAddAcrate() { return addAcrate; }
    public void setAddAcrate(Integer addAcrate) { this.addAcrate = addAcrate; }
    public Integer getAddMcrate() { return addMcrate; }
    public void setAddMcrate(Integer addMcrate) { this.addMcrate = addMcrate; }
    public Integer getAddHitsrate() { return addHitsrate; }
    public void setAddHitsrate(Integer addHitsrate) { this.addHitsrate = addHitsrate; }
    public Integer getAddMissrate() { return addMissrate; }
    public void setAddMissrate(Integer addMissrate) { this.addMissrate = addMissrate; }
    public Integer getAddSpeedrate() { return addSpeedrate; }
    public void setAddSpeedrate(Integer addSpeedrate) { this.addSpeedrate = addSpeedrate; }
    public Integer getDxsh() { return dxsh; }
    public void setDxsh(Integer dxsh) { this.dxsh = dxsh; }
    public Integer getHitshp() { return hitshp; }
    public void setHitshp(Integer hitshp) { this.hitshp = hitshp; }
    public Integer getHitsmp() { return hitsmp; }
    public void setHitsmp(Integer hitsmp) { this.hitsmp = hitsmp; }
    public Integer getShjs() { return shjs; }
    public void setShjs(Integer shjs) { this.shjs = shjs; }
    public Integer getSdmp() { return sdmp; }
    public void setSdmp(Integer sdmp) { this.sdmp = sdmp; }
    public Integer getSzmp() { return szmp; }
    public void setSzmp(Integer szmp) { this.szmp = szmp; }
    public Integer getAddmoney() { return addmoney; }
    public void setAddmoney(Integer addmoney) { this.addmoney = addmoney; }
    public Integer getTime() { return time; }
    public void setTime(Integer time) { this.time = time; }
}