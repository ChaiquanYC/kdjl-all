package com.kdjl.server.repository;

import com.kdjl.common.entity.ExpToLv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpToLvRepository extends JpaRepository<ExpToLv, Long> {
    ExpToLv findByLevel(Integer level);
}
