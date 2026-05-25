package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guild_settings")
public class GuildSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column private Integer level;
    @Column(name = "need_honor") private Integer needHonor;
    @Column(name = "need_props", length = 255) private String needProps;
    @Column(name = "need_member_number") private Integer needMemberNumber;
    @Column(name = "need_items_for_shop", length = 255) private String needItemsForShop;
    @Column(name = "max_member_number") private Integer maxMemberNumber;
    @Column(length = 255) private String welfare;
    public Long getId() { return id; }
    public Integer getLevel() { return level; }
    public Integer getNeedHonor() { return needHonor; }
    public String getNeedProps() { return needProps; }
    public Integer getNeedMemberNumber() { return needMemberNumber; }
    public String getNeedItemsForShop() { return needItemsForShop; }
    public Integer getMaxMemberNumber() { return maxMemberNumber; }
    public String getWelfare() { return welfare; }
}
