package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guild_bag")
public class GuildBag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "guild_id") private Long guildId;
    @Column(name = "pid") private Long propId;
    @Column private Integer sums;
    public Long getId() { return id; }
    public Long getGuildId() { return guildId; }
    public Long getPropId() { return propId; }
    public Integer getSums() { return sums; }
}
