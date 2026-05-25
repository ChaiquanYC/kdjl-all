package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wx")
public class Wx {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column private Integer j;  // 金抗成长
    @Column private Integer m;  // 木抗成长
    @Column private Integer s;  // 水抗成长
    @Column private Integer h;  // 火抗成长
    @Column private Integer t;  // 土抗成长
    @Column private Integer wx; // 五行ID
    @Column private Integer hp;
    @Column private Integer mp;
    @Column private Integer ac;
    @Column private Integer mc;
    @Column private Integer speed;
    @Column private Integer hits;
    @Column private Integer miss;

    public Integer getId() { return id; }
    public Integer getJ() { return j; } public Integer getM() { return m; }
    public Integer getS() { return s; } public Integer getH() { return h; }
    public Integer getT() { return t; } public Integer getWx() { return wx; }
    public Integer getHp() { return hp; } public Integer getMp() { return mp; }
    public Integer getAc() { return ac; } public Integer getMc() { return mc; }
    public Integer getSpeed() { return speed; } public Integer getHits() { return hits; }
    public Integer getMiss() { return miss; }
}
