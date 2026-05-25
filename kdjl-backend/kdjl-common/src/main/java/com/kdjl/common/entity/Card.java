package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "card")
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 255, nullable = false) private String name;
    @Column private Integer num;
    @Column(length = 255) private String npcmsg;
    @Column(length = 12) private String starttime;
    @Column(length = 12) private String endtime;
    @Column(length = 255) private String prize;
    @Column private Integer flag;
    @Column private Integer checked;
    @Column(columnDefinition = "text") private String info;
    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getNum() { return num; }
    public String getNpcmsg() { return npcmsg; }
    public String getStarttime() { return starttime; }
    public String getEndtime() { return endtime; }
    public String getPrize() { return prize; }
    public Integer getFlag() { return flag; }
    public Integer getChecked() { return checked; }
    public String getInfo() { return info; }
}
