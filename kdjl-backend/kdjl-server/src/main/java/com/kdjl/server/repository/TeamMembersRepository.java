package com.kdjl.server.repository;

import com.kdjl.common.entity.TeamMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMembersRepository extends JpaRepository<TeamMembers, TeamMembers.TeamMembersId> {
    List<TeamMembers> findByPlayerId(Long playerId);
    List<TeamMembers> findByTeamId(Long teamId);
    long countByTeamId(Long teamId);
    void deleteByTeamId(Long teamId);
}
