package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_team")
public class WarTeam {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creater_id") private Long creatorId;
    @Column(length = 30) private String name;
    @Column(name = "map_id") private Integer mapId;
    @Column(length = 32) private String password;
    @Column(name = "create_time") private Long createTime;
    @Column private Integer fighting;

    public Long getId() { return id; }
    public Long getCreatorId() { return creatorId; }
    public String getName() { return name; }
    public Integer getMapId() { return mapId; }
    public String getPassword() { return password; }
    public Long getCreateTime() { return createTime; }
    public Integer getFighting() { return fighting; }
}
