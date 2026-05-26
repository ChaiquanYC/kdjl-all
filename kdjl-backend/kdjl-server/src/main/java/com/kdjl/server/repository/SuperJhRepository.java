package com.kdjl.server.repository;

import com.kdjl.common.entity.SuperJh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperJhRepository extends JpaRepository<SuperJh, Integer> {
    Optional<SuperJh> findByPetId(Integer petId);
}
