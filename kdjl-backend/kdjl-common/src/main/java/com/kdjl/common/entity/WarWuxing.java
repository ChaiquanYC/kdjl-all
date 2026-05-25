package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_wuxing")
public class WarWuxing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "`key`") private Integer key;
    @Column(length = 50) private String name;
    @Column private Integer restraint;

    public Integer getId() { return id; }
    public Integer getKey() { return key; }
    public String getName() { return name; }
    public Integer getRestraint() { return restraint; }
}
