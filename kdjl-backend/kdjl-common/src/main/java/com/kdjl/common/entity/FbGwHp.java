package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fb_gw_hp")
public class FbGwHp {
    @Id @Column(name = "uid") private Long playerId;
    @Column(name = "bid") private Long petId;
    @Column(name = "gid") private Long monsterId;
    @Column private Long hp;
    @Column private Long mp;
    @Column private Integer fatting;
    @Column private Long ftime;
    @Column private Integer fuzu;
    @Column private Integer boss;
    public Long getPlayerId() { return playerId; }
    public Long getPetId() { return petId; }
    public Long getMonsterId() { return monsterId; }
    public Long getHp() { return hp; }
    public Long getMp() { return mp; }
    public Integer getFatting() { return fatting; }
    public Long getFtime() { return ftime; }
    public Integer getFuzu() { return fuzu; }
    public Integer getBoss() { return boss; }
}
