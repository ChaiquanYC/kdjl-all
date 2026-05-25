package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "map")
public class GameMap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 50) private String name;
    @Column(length = 255) private String descs;
    @Column(length = 255) private String gpclist;
    @Column(length = 50) private String level;
    @Column(length = 100) private String img;
    @Column(length = 255) private String needs;
    @Column(name = "multi_monsters", length = 100, nullable = false) private String multiMonsters;
    @Column(length = 15) private String czlprops;
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescs() { return descs; }
    public String getGpclist() { return gpclist; }
    public String getLevel() { return level; }
    public String getImg() { return img; }
    public String getNeeds() { return needs; }
    public String getMultiMonsters() { return multiMonsters; }
    public String getCzlprops() { return czlprops; }
}
