package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "tasklog")
public class TaskLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(name = "taskid") private Long taskId;
    @Column private Integer xulie;
    @Column private Long time;
    @Column private Integer fromnpc;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getTaskId() { return taskId; }
    public Integer getXulie() { return xulie; }
    public Long getTime() { return time; }
    public Integer getFromnpc() { return fromnpc; }
}
