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
}
