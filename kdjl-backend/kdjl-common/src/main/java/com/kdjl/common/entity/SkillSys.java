package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "skillsys")
public class SkillSys {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pid") private Long pid;
    @Column(length = 50) private String name;
    @Column(length = 10) private String vary;
    @Column private Integer wx;
    @Column(length = 20) private String img;
    @Column(length = 255) private String ackvalue;
    @Column(length = 255) private String plus;
    @Column(length = 255) private String requires;
    @Column(length = 255) private String uhp;
    @Column(length = 255) private String ump;
    @Column(length = 255) private String imgeft;
    @Column private Integer ackstyle;
    public Long getId() { return id; }
    public Long getPid() { return pid; }
    public String getName() { return name; }
    public String getVary() { return vary; }
    public Integer getWx() { return wx; }
    public String getImg() { return img; }
    public String getAckvalue() { return ackvalue; }
    public String getPlus() { return plus; }
    public String getRequires() { return requires; }
    public String getUhp() { return uhp; }
    public String getUmp() { return ump; }
    public String getImgeft() { return imgeft; }
    public Integer getAckstyle() { return ackstyle; }
}
