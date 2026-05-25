package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fuben")
public class Fuben {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column(name = "gwid") private Integer gwId;
    @Column private Long lttime;
    @Column private Integer inmap;
    @Column private Long srctime;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Integer getGwId() { return gwId; }
    public Long getLttime() { return lttime; }
    public Integer getInmap() { return inmap; }
    public Long getSrctime() { return srctime; }

    public void setId(Long id) { this.id = id; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setGwId(Integer gwId) { this.gwId = gwId; }
    public void setLttime(Long lttime) { this.lttime = lttime; }
    public void setInmap(Integer inmap) { this.inmap = inmap; }
    public void setSrctime(Long srctime) { this.srctime = srctime; }
}
