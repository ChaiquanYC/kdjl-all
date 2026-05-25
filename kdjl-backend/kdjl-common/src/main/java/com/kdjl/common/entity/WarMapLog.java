package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "war_map_log")
@IdClass(WarMapLog.WarMapLogId.class)
public class WarMapLog {
    @Id @Column(name = "map_id") private Long mapId;
    @Id @Column(name = "uid") private Long playerId;
    @Column private Long endline;
    public Long getMapId() { return mapId; }
    public Long getPlayerId() { return playerId; }
    public Long getEndline() { return endline; }

    public static class WarMapLogId implements java.io.Serializable {
        private Long mapId; private Long playerId;
        public WarMapLogId() {}
        @Override public int hashCode() { return (int)(mapId + playerId); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof WarMapLogId w)) return false;
            return mapId.equals(w.mapId) && playerId.equals(w.playerId);
        }
    }
}
