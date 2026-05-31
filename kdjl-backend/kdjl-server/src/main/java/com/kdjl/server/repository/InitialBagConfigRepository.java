package com.kdjl.server.repository;

import com.kdjl.common.entity.InitialBagConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InitialBagConfigRepository extends JpaRepository<InitialBagConfig, Integer> {
    List<InitialBagConfig> findByEnabledOrderBySortOrderAsc(Integer enabled);
}
