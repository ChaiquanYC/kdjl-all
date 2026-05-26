package com.kdjl.server.repository;

import com.kdjl.common.entity.UserBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBagRepository extends JpaRepository<UserBag, Long> {
    List<UserBag> findByPlayerId(Long playerId);
    List<UserBag> findByPlayerIdAndVary(Long playerId, Integer vary);
    List<UserBag> findByPlayerIdAndPropIdIn(Integer playerId, List<Integer> propIds);
    List<UserBag> findByEquipPetId(Long equipPetId);
    List<UserBag> findByPlayerIdAndEquipPetId(Long playerId, Long equipPetId);
    List<UserBag> findByPlayerIdAndPropIdInAndEquipPetId(Long playerId, List<Long> propIds, Long equipPetId);
}
