package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "challenge_log")
public class ChallengeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(name = "gid") private Long monsterId;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getMonsterId() { return monsterId; }
}
