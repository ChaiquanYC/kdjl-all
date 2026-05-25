package com.kdjl.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "t_shop_log")
public class TShopLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uid") private Long playerId;
    @Column(name = "product_id", length = 32) private String productId;
    @Column(name = "buy_number") private Integer buyNumber;
    @Column(name = "buy_price", precision = 10, scale = 2) private BigDecimal buyPrice;
    @Column(name = "buy_time") private Long buyTime;
    @Column private Integer trade;
    @Column(length = 4096) private String receipt;
    @Column(name = "item_id", length = 32) private String itemId;
    @Column(name = "transaction_id", length = 32) private String transactionId;
    @Column(name = "error_detail", length = 1024) private String errorDetail;
    @Column(name = "is_double") private Integer isDouble;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getProductId() { return productId; }
    public Integer getBuyNumber() { return buyNumber; }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public Long getBuyTime() { return buyTime; }
    public Integer getTrade() { return trade; }
    public String getReceipt() { return receipt; }
    public String getItemId() { return itemId; }
    public String getTransactionId() { return transactionId; }
    public String getErrorDetail() { return errorDetail; }
    public Integer getIsDouble() { return isDouble; }
}
