package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "team")
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 30) private String name;
    @Column private Long creator;
    @Column private Integer inmap;
    @Column(length = 255) private String monsters;
    @Column(name = "exp_get") private Long expGet;
    @Column(name = "props_get", length = 255) private String propsGet;
    @Column private Integer state;
    @Column(name = "create_time") private Long createTime;
    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getCreator() { return creator; }
    public Integer getInmap() { return inmap; }
    public String getMonsters() { return monsters; }
    public Long getExpGet() { return expGet; }
    public String getPropsGet() { return propsGet; }
    public Integer getState() { return state; }
    public Long getCreateTime() { return createTime; }
    public void setName(String name) { this.name = name; }
    public void setCreator(Long creator) { this.creator = creator; }
    public void setInmap(Integer inmap) { this.inmap = inmap; }
    public void setState(Integer state) { this.state = state; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
}
