package com.kdjl.server.repository;

import com.kdjl.common.entity.Welcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WelcomeRepository extends JpaRepository<Welcome, Integer> {
    Optional<Welcome> findByCode(String code);
}
