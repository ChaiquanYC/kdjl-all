package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "team_members")
@IdClass(TeamMembers.TeamMembersId.class)
public class TeamMembers {
    @Id @Column(name = "team_id") private Long teamId;
    @Id @Column(name = "uid") private Long playerId;
    @Column(length = 50) private String nickname;
    @Column private Integer state; // -1=pending, 0=away, 1=present
    @Column(name = "apply_time") private Long applyTime;
    @Column(name = "update_time") private Long updateTime;
    public Long getTeamId() { return teamId; }
    public Long getPlayerId() { return playerId; }
    public String getNickname() { return nickname; }
    public Integer getState() { return state; }
    public Long getApplyTime() { return applyTime; }
    public Long getUpdateTime() { return updateTime; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setState(Integer state) { this.state = state; }
    public void setApplyTime(Long applyTime) { this.applyTime = applyTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }

    public static class TeamMembersId implements java.io.Serializable {
        private Long teamId; private Long playerId;
        public TeamMembersId() {}
        public TeamMembersId(Long teamId, Long playerId) { this.teamId = teamId; this.playerId = playerId; }
        @Override public int hashCode() { return (int)(teamId + playerId); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof TeamMembersId t)) return false;
            return teamId.equals(t.teamId) && playerId.equals(t.playerId);
        }
    }
}
