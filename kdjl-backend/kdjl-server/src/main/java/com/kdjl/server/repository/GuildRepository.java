package com.kdjl.server.repository;

import com.kdjl.common.entity.Guild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildRepository extends JpaRepository<Guild, Long> {
    Optional<Guild> findByName(String name);
}
