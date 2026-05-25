package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_map")
public class WarMap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50) private String name;
    @Column(length = 255) private String descs;
    @Column(name = "enter_condition", length = 50) private String enterCondition;
    @Column(name = "`type`") private Integer type;
    @Column(length = 254) private String opened;
    @Column(name = "difficult_level", length = 20) private String difficultLevel;
    @Column(name = "cooling_time") private Integer coolingTime;
    @Column(name = "player_num") private Integer playerNum;
    @Column(name = "wuxing_damage") private Integer wuxingDamage;
    @Column(length = 50) private String img;
    @Column(name = "start_battle_id") private Integer startBattleId;
    @Column(length = 50) private String teamimg;

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getDescs() { return descs; }
    public String getEnterCondition() { return enterCondition; }
    public Integer getType() { return type; }
    public String getOpened() { return opened; }
    public String getDifficultLevel() { return difficultLevel; }
    public Integer getCoolingTime() { return coolingTime; }
    public Integer getPlayerNum() { return playerNum; }
    public Integer getWuxingDamage() { return wuxingDamage; }
    public String getImg() { return img; }
    public Integer getStartBattleId() { return startBattleId; }
    public String getTeamimg() { return teamimg; }
}
