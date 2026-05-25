package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "moneylog")
public class MoneyLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column private Long times;
    @Column(length = 100) private String money;
    public Long getId() { return id; }
    public Long getTimes() { return times; }
    public String getMoney() { return money; }
}
