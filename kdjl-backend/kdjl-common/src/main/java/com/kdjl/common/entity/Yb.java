package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "yb")
public class Yb {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "Id") private Long id;
    @Column(length = 40) private String payname;
    @Column private Long paytime;
    @Column(length = 20) private String paymoney;
    @Column private Long getyb;
    @Column(length = 25) private String orderid;
    public Long getId() { return id; }
    public String getPayname() { return payname; }
    public Long getPaytime() { return paytime; }
    public String getPaymoney() { return paymoney; }
    public Long getGetyb() { return getyb; }
    public String getOrderid() { return orderid; }
}
