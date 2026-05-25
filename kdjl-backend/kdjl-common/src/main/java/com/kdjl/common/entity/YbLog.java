package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "yblog")
public class YbLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 255) private String title;
    @Column(length = 50) private String nickname;
    @Column private Long yb;
    @Column private Long buytime;
    @Column(length = 50) private String pname;
    @Column private Integer nums;
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getNickname() { return nickname; }
    public Long getYb() { return yb; }
    public Long getBuytime() { return buytime; }
    public String getPname() { return pname; }
    public Integer getNums() { return nums; }
}
