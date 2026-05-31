package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "initial_bag_config")
public class InitialBagConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "prop_id", nullable = false)
    private Long propId;

    @Column(nullable = false)
    private Integer count = 1;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer enabled = 1;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Long getPropId() { return propId; }
    public void setPropId(Long propId) { this.propId = propId; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
