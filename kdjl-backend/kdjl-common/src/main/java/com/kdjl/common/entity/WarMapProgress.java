package com.kdjl.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "war_map_progress")
@IdClass(WarMapProgress.WarMapProgressId.class)
public class WarMapProgress {

    @Id @Column(name = "player_id") private Long playerId;
    @Id @Column(name = "map_id") private Integer mapId;

    @Column(name = "team_id") private Long teamId;
    @Column(name = "battle_id") private Integer battleId;
    @Column(name = "last_pass_time") private LocalDateTime lastPassTime;

    public Long getPlayerId() { return playerId; }
    public Integer getMapId() { return mapId; }
    public Long getTeamId() { return teamId; }
    public Integer getBattleId() { return battleId; }
    public LocalDateTime getLastPassTime() { return lastPassTime; }

    public static class WarMapProgressId implements java.io.Serializable {
        private Long playerId;
        private Integer mapId;
        public WarMapProgressId() {}
        @Override public int hashCode() { return (int)(playerId + mapId); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof WarMapProgressId w)) return false;
            return playerId.equals(w.playerId) && mapId.equals(w.mapId);
        }
    }
}
