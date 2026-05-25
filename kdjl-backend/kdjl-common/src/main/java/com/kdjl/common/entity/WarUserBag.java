package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_user_bag")
public class WarUserBag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid") private Long playerId;
    @Column(name = "pid") private Long propId;
    @Column(length = 50) private String name;
    @Column(name = "talent_effect_code", length = 20) private String talentEffectCode;
    @Column private Integer sums;
    @Column(name = "`desc`", length = 255) private String description;
    @Column private Integer sales;
    @Column(length = 80) private String img;
    @Column private Integer color;

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public Long getPropId() { return propId; }
    public String getName() { return name; }
    public String getTalentEffectCode() { return talentEffectCode; }
    public Integer getSums() { return sums; }
    public String getDescription() { return description; }
    public Integer getSales() { return sales; }
    public String getImg() { return img; }
    public Integer getColor() { return color; }
}
