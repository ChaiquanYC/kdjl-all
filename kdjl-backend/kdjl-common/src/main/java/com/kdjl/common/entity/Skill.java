package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid")
    private Long petId;

    @Column(name = "sid")
    private Long skillDefId;

    @Column(length = 50)
    private String name;

    @Column
    private Integer level;

    @Column(length = 50)
    private String vary;

    @Column
    private Integer wx; // element

    @Column(name = "`value`", length = 50)
    private String value;

    @Column(length = 100)
    private String plus;

    @Column(length = 50)
    private String img;

    @Column
    private Integer uhp;

    @Column
    private Integer ump;

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getSkillDefId() { return skillDefId; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public String getVary() { return vary; }
    public Integer getWx() { return wx; }
    public String getValue() { return value; }
    public String getPlus() { return plus; }
    public String getImg() { return img; }
    public Integer getUhp() { return uhp; }
    public Integer getUmp() { return ump; }

    public void setPetId(Long petId) { this.petId = petId; }
    public void setSkillDefId(Long skillDefId) { this.skillDefId = skillDefId; }
    public void setName(String name) { this.name = name; }
    public void setLevel(Integer level) { this.level = level; }
    public void setVary(String vary) { this.vary = vary; }
    public void setWx(Integer wx) { this.wx = wx; }
    public void setValue(String value) { this.value = value; }
    public void setPlus(String plus) { this.plus = plus; }
    public void setImg(String img) { this.img = img; }
    public void setUhp(Integer uhp) { this.uhp = uhp; }
    public void setUmp(Integer ump) { this.ump = ump; }
}
