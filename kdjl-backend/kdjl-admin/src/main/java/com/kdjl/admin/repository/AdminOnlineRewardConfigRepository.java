package com.kdjl.admin.repository;

import com.kdjl.common.entity.OnlineRewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminOnlineRewardConfigRepository extends JpaRepository<OnlineRewardConfig, Integer> {
    List<OnlineRewardConfig> findAllByOrderByStepAscLevelMinAsc();
    List<OnlineRewardConfig> findByStepOrderByLevelMinAsc(Integer step);
}
