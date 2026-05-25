package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "battlefield_props")
public class BattlefieldProps {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pid") private Long propId;
    @Column private Integer need;
    public Long getId() { return id; }
    public Long getPropId() { return propId; }
    public Integer getNeed() { return need; }
}
