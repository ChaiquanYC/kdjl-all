package com.kdjl.server.repository;

import com.kdjl.common.entity.OnlineRewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnlineRewardConfigRepository extends JpaRepository<OnlineRewardConfig, Integer> {
    List<OnlineRewardConfig> findAllByOrderByStepAscLevelMinAsc();
    List<OnlineRewardConfig> findByStepOrderByLevelMinAsc(Integer step);
}
