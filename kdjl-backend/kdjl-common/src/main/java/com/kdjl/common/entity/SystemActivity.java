package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "system_activity")
public class SystemActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 255, nullable = false) private String title;
    @Column(length = 255, nullable = false) private String time;
    @Column(nullable = false) private Integer week;
    @Column(length = 255, nullable = false) private String pic;
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTime() { return time; }
    public Integer getWeek() { return week; }
    public String getPic() { return pic; }
}
