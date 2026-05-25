package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "tarot")
public class Tarot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 20) private String name;
    @Column private Long sj;
    @Column(length = 255) private String effect;
    @Column private Integer flag;
    @Column(length = 255) private String content;
    @Column(length = 100) private String boss;
    @Column(length = 255) private String img;
    @Column(name = "mapid") private Long mapId;
    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getSj() { return sj; }
    public String getEffect() { return effect; }
    public Integer getFlag() { return flag; }
    public String getContent() { return content; }
    public String getBoss() { return boss; }
    public String getImg() { return img; }
    public Long getMapId() { return mapId; }
}
