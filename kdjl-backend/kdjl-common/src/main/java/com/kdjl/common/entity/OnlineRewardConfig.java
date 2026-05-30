package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "online_reward_config")
public class OnlineRewardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer step;

    @Column(name = "level_min", nullable = false)
    private Integer levelMin;

    @Column(name = "level_max", nullable = false)
    private Integer levelMax;

    @Column(name = "time_minutes", nullable = false)
    private Integer timeMinutes;

    @Column(name = "item_ids", nullable = false, length = 255)
    private String itemIds;

    @Column(name = "item_counts", nullable = false, length = 255)
    private String itemCounts;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public Integer getId() { return id; }
    public Integer getStep() { return step; }
    public Integer getLevelMin() { return levelMin; }
    public Integer getLevelMax() { return levelMax; }
    public Integer getTimeMinutes() { return timeMinutes; }
    public String getItemIds() { return itemIds; }
    public String getItemCounts() { return itemCounts; }
    public Integer getSortOrder() { return sortOrder; }

    public void setId(Integer id) { this.id = id; }
    public void setStep(Integer step) { this.step = step; }
    public void setLevelMin(Integer levelMin) { this.levelMin = levelMin; }
    public void setLevelMax(Integer levelMax) { this.levelMax = levelMax; }
    public void setTimeMinutes(Integer timeMinutes) { this.timeMinutes = timeMinutes; }
    public void setItemIds(String itemIds) { this.itemIds = itemIds; }
    public void setItemCounts(String itemCounts) { this.itemCounts = itemCounts; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
