package com.kdjl.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_log")
public class AuctionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "seller_name", length = 50)
    private String sellerName;

    @Column(name = "buyer_id")
    private Integer buyerId;

    @Column(name = "buyer_name", length = 50)
    private String buyerName;

    @Column(name = "prop_id", nullable = false)
    private Long propId;

    @Column(name = "prop_name", length = 100)
    private String propName;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "price_type", length = 10)
    private String priceType;

    @Column(nullable = false)
    private String action;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public Long getPropId() { return propId; }
    public void setPropId(Long propId) { this.propId = propId; }

    public String getPropName() { return propName; }
    public void setPropName(String propName) { this.propName = propName; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getPriceType() { return priceType; }
    public void setPriceType(String priceType) { this.priceType = priceType; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
