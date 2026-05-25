package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "welcome")
public class Welcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String code;

    @Column(name = "value2", length = 255)
    private String value2;

    @Column(columnDefinition = "TEXT")
    private String contents;

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getValue2() { return value2; }
    public String getContents() { return contents; }
}
