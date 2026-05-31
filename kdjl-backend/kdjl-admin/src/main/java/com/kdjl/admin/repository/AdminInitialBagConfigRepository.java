package com.kdjl.admin.repository;

import com.kdjl.common.entity.InitialBagConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminInitialBagConfigRepository extends JpaRepository<InitialBagConfig, Integer> {
    List<InitialBagConfig> findAllByOrderBySortOrderAsc();
}
