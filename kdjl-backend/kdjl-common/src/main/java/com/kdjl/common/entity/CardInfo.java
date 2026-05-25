package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "card_info")
public class CardInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column private Integer cardtype;
    @Column(length = 30) private String cardid;
    @Column(length = 10) private String pwd;
    @Column private Integer checked;
    @Column(name = "uid") private Long playerId;
    @Column(length = 12) private String times;
    @Column(name = "mbid") private Long mbid;
    public Long getId() { return id; }
    public Integer getCardtype() { return cardtype; }
    public String getCardid() { return cardid; }
    public String getPwd() { return pwd; }
    public Integer getChecked() { return checked; }
    public Long getPlayerId() { return playerId; }
    public String getTimes() { return times; }
    public Long getMbid() { return mbid; }
}
