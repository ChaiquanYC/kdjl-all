package com.kdjl.admin.repository;

import com.kdjl.common.entity.UserPet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminUserPetRepository extends JpaRepository<UserPet, Long> {
    List<UserPet> findByPlayerId(Long playerId);
}
