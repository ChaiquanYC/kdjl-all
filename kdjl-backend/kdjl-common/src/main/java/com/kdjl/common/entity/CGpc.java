package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "c_gpc")
public class CGpc {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255) private String gpc;
    @Column private Integer boss;
    @Column(length = 255) private String drops;
    @Column(name = "map_id") private Integer mapId;
    @Column(name = "step_id") private Integer stepId;
    @Column(name = "group_id") private Integer groupId;

    public Long getId() { return id; }
    public String getGpc() { return gpc; }
    public Integer getBoss() { return boss; }
    public String getDrops() { return drops; }
    public Integer getMapId() { return mapId; }
    public Integer getStepId() { return stepId; }
    public Integer getGroupId() { return groupId; }
}
