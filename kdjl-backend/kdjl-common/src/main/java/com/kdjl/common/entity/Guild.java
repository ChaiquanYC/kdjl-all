package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guild")
public class Guild {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50) private String name;
    @Column(length = 255) private String info;
    @Column(name = "creator_id", length = 255) private String creatorId;
    @Column(name = "president_id") private Long presidentId;
    @Column private Integer honor;
    @Column private Integer level;
    @Column(name = "shop_level") private Integer shopLevel;
    @Column(name = "number_of_member") private Integer memberCount;
    @Column(name = "create_time") private Long createTime;
    @Column(name = "victory_times") private Integer victoryTimes;
    @Column(name = "failed_times") private Integer failedTimes;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getInfo() { return info; }
    public String getCreatorId() { return creatorId; }
    public Long getPresidentId() { return presidentId; }
    public Integer getHonor() { return honor; }
    public Integer getLevel() { return level; }
    public Integer getShopLevel() { return shopLevel; }
    public Integer getMemberCount() { return memberCount; }
    public Long getCreateTime() { return createTime; }
    public Integer getVictoryTimes() { return victoryTimes; }
    public Integer getFailedTimes() { return failedTimes; }
    public void setName(String name) { this.name = name; }
    public void setInfo(String info) { this.info = info; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setPresidentId(Long presidentId) { this.presidentId = presidentId; }
    public void setHonor(Integer honor) { this.honor = honor; }
    public void setLevel(Integer level) { this.level = level; }
    public void setShopLevel(Integer shopLevel) { this.shopLevel = shopLevel; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
}
