package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity @Table(name = "task_accept")
public class TaskAccept {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(name = "taskid") private Long taskId;
    @Column(length = 255) private String state;
    @Column(length = 255) private String comself;
    @Column private Long time;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getTaskId() { return taskId; }
    public String getState() { return state; }
    public String getComself() { return comself; }
    public Long getTime() { return time; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public void setState(String state) { this.state = state; }
    public void setComself(String comself) { this.comself = comself; }
    public void setTime(Long time) { this.time = time; }
}
