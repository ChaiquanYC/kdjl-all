package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shop_order")
public class ShopOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(length = 60) private String uname;
    @Column(name = "pid") private Long propId;
    @Column private Integer pnum;
    @Column private Integer fee;
    @Column(name = "order_id", length = 25) private String orderId;
    @Column(name = "create_time") private Long createTime;
    @Column private Integer flag;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getUname() { return uname; }
    public Long getPropId() { return propId; }
    public Integer getPnum() { return pnum; }
    public Integer getFee() { return fee; }
    public String getOrderId() { return orderId; }
    public Long getCreateTime() { return createTime; }
    public Integer getFlag() { return flag; }

    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setUname(String uname) { this.uname = uname; }
    public void setPropId(Long propId) { this.propId = propId; }
    public void setPnum(Integer pnum) { this.pnum = pnum; }
    public void setFee(Integer fee) { this.fee = fee; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
    public void setFlag(Integer flag) { this.flag = flag; }
}
