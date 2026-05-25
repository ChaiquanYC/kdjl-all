package com.kdjl.server.repository;

import com.kdjl.common.entity.Wx;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WxRepository extends JpaRepository<Wx, Integer> {
    Wx findByWx(Integer wx);
}
