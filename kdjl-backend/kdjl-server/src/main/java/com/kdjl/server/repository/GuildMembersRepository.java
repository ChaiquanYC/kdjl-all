package com.kdjl.server.repository;

import com.kdjl.common.entity.GuildMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildMembersRepository extends JpaRepository<GuildMembers, Long> {
    Optional<GuildMembers> findByMemberId(Long memberId);
    List<GuildMembers> findByGuildId(Long guildId);
}
