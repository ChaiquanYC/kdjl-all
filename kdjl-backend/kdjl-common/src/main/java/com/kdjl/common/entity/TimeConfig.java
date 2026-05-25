package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "timeconfig")
public class TimeConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column(length = 255) private String titles;
    @Column(length = 255) private String days;
    @Column(length = 50) private String starttime;
    @Column(length = 50) private String endtime;
    public Long getId() { return id; }
    public String getTitles() { return titles; }
    public String getDays() { return days; }
    public String getStarttime() { return starttime; }
    public String getEndtime() { return endtime; }
}
