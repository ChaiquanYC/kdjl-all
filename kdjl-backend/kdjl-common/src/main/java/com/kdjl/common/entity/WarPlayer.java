package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_player")
public class WarPlayer {

    @Id
    @Column(name = "id")
    private Long playerId;

    @Column(length = 50) private String name;
    @Column private Integer level;
    @Column private Integer attack;
    @Column private Integer defense;
    @Column private Integer hp;
    @Column(name = "war_status") private Integer warStatus;
    @Column private Integer wuxing;
    @Column(name = "max_take_pet_num") private Integer maxTakePetNum;
    @Column(name = "max_take_pet_num_save") private Integer maxTakePetNumSave;
    @Column(name = "take_pet_limit_time") private Integer takePetLimitTime;
    @Column(name = "grow_up") private Integer growUp;
    @Column(name = "used_talent_times") private Integer usedTalentTimes;
    @Column(name = "wash_talent_count") private Integer washTalentCount;
    @Column(name = "last_enter_war_time") private Integer lastEnterWarTime;

    public Long getPlayerId() { return playerId; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public Integer getAttack() { return attack; }
    public Integer getDefense() { return defense; }
    public Integer getHp() { return hp; }
    public Integer getWarStatus() { return warStatus; }
    public Integer getWuxing() { return wuxing; }
    public Integer getMaxTakePetNum() { return maxTakePetNum; }
    public void setMaxTakePetNum(Integer v) { this.maxTakePetNum = v; }
    public Integer getMaxTakePetNumSave() { return maxTakePetNumSave; }
    public void setMaxTakePetNumSave(Integer v) { this.maxTakePetNumSave = v; }
    public Integer getTakePetLimitTime() { return takePetLimitTime; }
    public void setTakePetLimitTime(Integer v) { this.takePetLimitTime = v; }
    public Integer getGrowUp() { return growUp; }
    public Integer getUsedTalentTimes() { return usedTalentTimes; }
    public Integer getWashTalentCount() { return washTalentCount; }
    public void setWashTalentCount(Integer v) { this.washTalentCount = v; }
    public Integer getLastEnterWarTime() { return lastEnterWarTime; }
}
