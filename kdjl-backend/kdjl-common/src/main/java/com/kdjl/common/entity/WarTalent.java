package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_talent")
public class WarTalent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50) private String name;
    @Column(length = 255) private String descs;
    @Column(name = "is_group_damage") private Integer isGroupDamage;
    @Column(name = "is_long_distance_attack") private Integer isLongDistanceAttack;
    @Column(name = "talent_effect_code", length = 80) private String talentEffectCode;
    @Column(name = "magnification_1") private Integer magnification1;
    @Column(name = "magnification_2", length = 80) private String magnification2;
    @Column(name = "broadcast_text", length = 100) private String broadcastText;
    @Column(name = "can_up_grade") private Integer canUpGrade;
    @Column private Integer wuxing;
    @Column(name = "wash_order") private Integer washOrder;
    @Column(name = "`wash_type`") private Integer washType;
    @Column(name = "magnification_base", length = 30) private String magnificationBase;
    @Column(name = "wash_probability") private Integer washProbability;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescs() { return descs; }
    public Integer getIsGroupDamage() { return isGroupDamage; }
    public Integer getIsLongDistanceAttack() { return isLongDistanceAttack; }
    public String getTalentEffectCode() { return talentEffectCode; }
    public Integer getMagnification1() { return magnification1; }
    public String getMagnification2() { return magnification2; }
    public String getBroadcastText() { return broadcastText; }
    public Integer getCanUpGrade() { return canUpGrade; }
    public Integer getWuxing() { return wuxing; }
    public Integer getWashOrder() { return washOrder; }
    public Integer getWashType() { return washType; }
    public String getMagnificationBase() { return magnificationBase; }
    public Integer getWashProbability() { return washProbability; }
}
