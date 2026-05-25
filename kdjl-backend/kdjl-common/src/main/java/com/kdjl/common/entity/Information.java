package com.kdjl.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "information")
public class Information {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column private LocalDateTime times;
    @Column(columnDefinition = "text") private String content;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public LocalDateTime getTimes() { return times; }
    public String getContent() { return content; }
}
