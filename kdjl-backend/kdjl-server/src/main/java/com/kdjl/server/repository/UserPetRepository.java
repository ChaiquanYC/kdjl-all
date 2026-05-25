package com.kdjl.server.repository;

import com.kdjl.common.entity.UserPet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPetRepository extends JpaRepository<UserPet, Long> {
    List<UserPet> findByPlayerId(Long playerId);
    Optional<UserPet> findByName(String name);
}
