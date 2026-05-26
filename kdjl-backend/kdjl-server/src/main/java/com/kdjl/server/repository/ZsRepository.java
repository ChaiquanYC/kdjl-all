package com.kdjl.server.repository;

import com.kdjl.common.entity.Zs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZsRepository extends JpaRepository<Zs, Long> {
    Optional<Zs> findByAidAndBid(Integer aid, Integer bid);
}
