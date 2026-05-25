package com.kdjl.admin.repository;

import com.kdjl.common.entity.UserBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminUserBagRepository extends JpaRepository<UserBag, Long> {
    List<UserBag> findByPlayerId(Long playerId);
}
