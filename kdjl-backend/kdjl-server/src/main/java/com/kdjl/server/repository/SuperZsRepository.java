package com.kdjl.server.repository;

import com.kdjl.common.entity.SuperZs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperZsRepository extends JpaRepository<SuperZs, Integer> {
    List<SuperZs> findByCurPetId(Integer curPetId);
}
