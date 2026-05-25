package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "gonggao")
public class Gonggao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column(length = 12) private String starttime;
    @Column(length = 12) private String endtime;
    @Column private Integer times;
    @Column(columnDefinition = "text") private String msg;
    public Long getId() { return id; }
    public String getStarttime() { return starttime; }
    public String getEndtime() { return endtime; }
    public Integer getTimes() { return times; }
    public String getMsg() { return msg; }
}
