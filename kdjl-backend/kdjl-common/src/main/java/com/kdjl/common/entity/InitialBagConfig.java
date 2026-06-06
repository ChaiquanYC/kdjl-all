package com.kdjl.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Long getPropId() { return propId; }
    public void setPropId(Long propId) { this.propId = propId; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
