package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_fighter_talent")
public class WarFighterTalent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fighter_id") private Long fighterId;
    @Column(name = "talent_id") private Integer talentId;
    @Column(name = "talent_sign") private String talentSign;
    @Column private Integer level;
    @Column(name = "base_n") private Integer baseN;
    @Column(name = "current_experience") private Integer currentExperience;
    @Column(name = "level_up_experience") private Integer levelUpExperience;

    public Long getId() { return id; }
    public Long getFighterId() { return fighterId; }
    public Integer getTalentId() { return talentId; }
    public String getTalentSign() { return talentSign; }
    public Integer getLevel() { return level; }
    public Integer getBaseN() { return baseN; }
    public Integer getCurrentExperience() { return currentExperience; }
    public void setCurrentExperience(Integer v) { this.currentExperience = v; }
    public Integer getLevelUpExperience() { return levelUpExperience; }
    public void setLevelUpExperience(Integer v) { this.levelUpExperience = v; }
}
