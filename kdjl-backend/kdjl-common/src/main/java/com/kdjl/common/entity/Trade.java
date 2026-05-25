package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trade")
public class Trade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(length = 16) private String name;
    @Column(length = 255) private String myprops;
    @Column(length = 255) private String changeprops;
    @Column private Integer money;
    @Column private Long buyer;
    @Column private Long times;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getMyprops() { return myprops; }
    public String getChangeprops() { return changeprops; }
    public Integer getMoney() { return money; }
    public Long getBuyer() { return buyer; }
    public Long getTimes() { return times; }
}
