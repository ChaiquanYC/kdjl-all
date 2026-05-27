package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "battlefield_user")
public class BattlefieldUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column private Integer pos;
    @Column(name = "bid") private Long battlefieldId;
    @Column private Long jgvalue;
    @Column(length = 50) private String levels;
    @Column private Long addjgvalue;
    @Column private Long ackvalue;
    @Column private Long failjgvalue;
    @Column private Long failackvalue;
    @Column private Long lastvtime;
    @Column private Integer doublejg;
    @Column private Integer tops;
    @Column private Long curjgvalue;
    @Column private Integer boxnum;
    @Column private Integer nscf;
    @Column private Long subhp;
    @Column private Long addhp;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Integer getPos() { return pos; }
    public Long getBattlefieldId() { return battlefieldId; }
    public Long getJgvalue() { return jgvalue; }
    public String getLevels() { return levels; }
    public Long getAddjgvalue() { return addjgvalue; }
    public Long getAckvalue() { return ackvalue; }
    public Long getFailjgvalue() { return failjgvalue; }
    public Long getFailackvalue() { return failackvalue; }
    public Long getLastvtime() { return lastvtime; }
    public Integer getDoublejg() { return doublejg; }
    public Integer getTops() { return tops; }
    public Long getCurjgvalue() { return curjgvalue; }
    public Integer getBoxnum() { return boxnum; }
    public Integer getNscf() { return nscf; }
    public Long getSubhp() { return subhp; }
    public Long getAddhp() { return addhp; }

    public void setId(Long id) { this.id = id; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setPos(Integer pos) { this.pos = pos; }
    public void setBattlefieldId(Long battlefieldId) { this.battlefieldId = battlefieldId; }
    public void setJgvalue(Long jgvalue) { this.jgvalue = jgvalue; }
    public void setLevels(String levels) { this.levels = levels; }
    public void setAddjgvalue(Long addjgvalue) { this.addjgvalue = addjgvalue; }
    public void setAckvalue(Long ackvalue) { this.ackvalue = ackvalue; }
    public void setFailjgvalue(Long failjgvalue) { this.failjgvalue = failjgvalue; }
    public void setFailackvalue(Long failackvalue) { this.failackvalue = failackvalue; }
    public void setLastvtime(Long lastvtime) { this.lastvtime = lastvtime; }
    public void setDoublejg(Integer doublejg) { this.doublejg = doublejg; }
    public void setTops(Integer tops) { this.tops = tops; }
    public void setCurjgvalue(Long curjgvalue) { this.curjgvalue = curjgvalue; }
    public void setBoxnum(Integer boxnum) { this.boxnum = boxnum; }
    public void setNscf(Integer nscf) { this.nscf = nscf; }
    public void setSubhp(Long subhp) { this.subhp = subhp; }
    public void setAddhp(Long addhp) { this.addhp = addhp; }
}
