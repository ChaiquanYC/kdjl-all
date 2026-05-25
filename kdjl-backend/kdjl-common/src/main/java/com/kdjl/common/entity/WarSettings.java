package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_settings")
public class WarSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(length = 50) private String code;
    @Column(name = "`value`", length = 255) private String value;
    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getValue() { return value; }
}
