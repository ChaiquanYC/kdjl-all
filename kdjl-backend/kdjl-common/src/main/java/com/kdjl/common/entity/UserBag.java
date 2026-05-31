package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "userbag")
public class UserBag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pid")
    private Long propId;

    @Column(name = "uid")
    private Long playerId;

    @Column
    private Integer sell;

    @Column
    private Integer vary; // 1=item, 2=equipment

    @Column
    private Integer sums;

    @Column
    private Long stime;

    @Column
    private Integer psell;

    @Column
    private Long pstime;

    @Column
    private Long petime;

    @Column
    private Integer psum;

    @Column(length = 255)
    private String psj;

    @Column
    private Integer bsum;

    @Column
    private Integer zbing;

    @Column(name = "zbpets")
    private Long equipPetId;

    @Column(name = "buycode")
    private Long buyCode;

    @Column(name = "plus_tms_eft", length = 255)
    private String plusTimesEffect;

    @Column
    private Integer cantrade;

    @Column(name = "F_item_hole_info", length = 30)
    private String holeInfo;

    @Column
    private Integer pyb = 0; // auction ingot

    public Long getId() { return id; }
    public Long getPropId() { return propId; }
    public Long getPlayerId() { return playerId; }
    public Integer getSell() { return sell; }
    public Integer getVary() { return vary; }
    public Integer getSums() { return sums; }
    public Long getStime() { return stime; }
    public Integer getPsell() { return psell; }
    public Long getPstime() { return pstime; }
    public Long getPetime() { return petime; }
    public Integer getPsum() { return psum; }
    public String getPsj() { return psj; }
    public Integer getBsum() { return bsum; }
    public Integer getZbing() { return zbing; }
    public Long getEquipPetId() { return equipPetId; }
    public Long getBuyCode() { return buyCode; }
    public String getPlusTimesEffect() { return plusTimesEffect; }
    public Integer getCantrade() { return cantrade; }
    public String getHoleInfo() { return holeInfo; }
    public Integer getPyb() { return pyb; }

    public void setSums(Integer sums) { this.sums = sums; }
    public void setZbing(Integer zbing) { this.zbing = zbing; }
    public void setEquipPetId(Long equipPetId) { this.equipPetId = equipPetId; }
    public void setSell(Integer sell) { this.sell = sell; }
    public void setPropId(Long propId) { this.propId = propId; }
    public void setVary(Integer vary) { this.vary = vary; }
    public void setCantrade(Integer cantrade) { this.cantrade = cantrade; }
    public void setStime(Long stime) { this.stime = stime; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setPyb(Integer pyb) { this.pyb = pyb; }
    public void setPsell(Integer psell) { this.psell = psell; }
    public void setPstime(Long pstime) { this.pstime = pstime; }
    public void setBsum(Integer bsum) { this.bsum = bsum; }
    public void setPetime(Long petime) { this.petime = petime; }
    public void setPsum(Integer psum) { this.psum = psum; }
    public void setPsj(String psj) { this.psj = psj; }
    public void setHoleInfo(String holeInfo) { this.holeInfo = holeInfo; }
    public void setPlusTimesEffect(String plusTimesEffect) { this.plusTimesEffect = plusTimesEffect; }
    public void setBuyCode(Long buyCode) { this.buyCode = buyCode; }
}
