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
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFromnpc() { return fromnpc; }
    public void setFromnpc(String fromnpc) { this.fromnpc = fromnpc; }
    public String getFrommsg() { return frommsg; }
    public void setFrommsg(String frommsg) { this.frommsg = frommsg; }
    public String getOkmsg() { return okmsg; }
    public void setOkmsg(String okmsg) { this.okmsg = okmsg; }
    public Integer getOknpc() { return oknpc; }
    public void setOknpc(Integer oknpc) { this.oknpc = oknpc; }
    public String getOkneed() { return okneed; }
    public void setOkneed(String okneed) { this.okneed = okneed; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }
    public String getLimitlv() { return limitlv; }
    public void setLimitlv(String limitlv) { this.limitlv = limitlv; }
    public Integer getHide() { return hide; }
    public void setHide(Integer hide) { this.hide = hide; }
    public Integer getXulie() { return xulie; }
    public void setXulie(Integer xulie) { this.xulie = xulie; }
    public Integer getColor() { return color; }
    public void setColor(Integer color) { this.color = color; }
    public Integer getFlags() { return flags; }
    public void setFlags(Integer flags) { this.flags = flags; }
}
