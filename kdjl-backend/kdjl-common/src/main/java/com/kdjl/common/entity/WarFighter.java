package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_fighter")
@IdClass(WarFighter.WarFighterId.class)
public class WarFighter {

    @Id @Column(name = "team_id") private Long teamId;
    @Id @Column(name = "user_id") private Long userId;
    @Id @Column(name = "fighter_id") private Long fighterId;

    @Column(length = 50) private String name;
    @Column private Integer level;
    @Column private Integer attack;
    @Column private Integer defense;
    @Column private Integer fullhp;
    @Column private Integer hp;
    @Column(length = 50) private String img;
    @Column private Integer wuxing;
    @Column(length = 50) private String imgack;

    public Long getTeamId() { return teamId; }
    public Long getUserId() { return userId; }
    public Long getFighterId() { return fighterId; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public Integer getAttack() { return attack; }
    public Integer getDefense() { return defense; }
    public Integer getFullhp() { return fullhp; }
    public Integer getHp() { return hp; }
    public String getImg() { return img; }
    public Integer getWuxing() { return wuxing; }
    public String getImgack() { return imgack; }

    public static class WarFighterId implements java.io.Serializable {
        private Long teamId;
        private Long userId;
        private Long fighterId;
        public WarFighterId() {}
        @Override public int hashCode() { return (int)(teamId + userId + fighterId); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof WarFighterId w)) return false;
            return teamId.equals(w.teamId) && userId.equals(w.userId) && fighterId.equals(w.fighterId);
        }
    }
}
