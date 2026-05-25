package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "challenge")
public class Challenge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column private Long lastvtime;
    @Column private Integer nums;
    @Column(name = "gid") private Long monsterId;
    @Column private Integer vary;
    @Column private Integer snums;
    @Column private Integer flag;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getLastvtime() { return lastvtime; }
    public Integer getNums() { return nums; }
    public Long getMonsterId() { return monsterId; }
    public Integer getVary() { return vary; }
    public Integer getSnums() { return snums; }
    public Integer getFlag() { return flag; }
    public void setPlayerId(Long v) { this.playerId = v; }
    public void setLastvtime(Long v) { this.lastvtime = v; }
    public void setNums(Integer v) { this.nums = v; }
    public void setMonsterId(Long v) { this.monsterId = v; }
    public void setVary(Integer v) { this.vary = v; }
    public void setSnums(Integer v) { this.snums = v; }
    public void setFlag(Integer v) { this.flag = v; }
}
