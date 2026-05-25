package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "task")
public class TaskDef {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 100) private String title;
    @Column(length = 10) private String fromnpc;
    @Column(columnDefinition = "text") private String frommsg;
    @Column(length = 255) private String okmsg;
    @Column private Integer oknpc;
    @Column(length = 255) private String okneed;
    @Column(length = 255) private String result;
    @Column(length = 50) private String cid;
    @Column(length = 255) private String limitlv;
    @Column private Integer hide;
    @Column private Integer xulie;
    @Column private Integer color;
    @Column private Integer flags;
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getFromnpc() { return fromnpc; }
    public String getFrommsg() { return frommsg; }
    public String getOkmsg() { return okmsg; }
    public Integer getOknpc() { return oknpc; }
    public String getOkneed() { return okneed; }
    public String getResult() { return result; }
    public String getCid() { return cid; }
    public String getLimitlv() { return limitlv; }
    public Integer getHide() { return hide; }
    public Integer getXulie() { return xulie; }
    public Integer getColor() { return color; }
    public Integer getFlags() { return flags; }
}
