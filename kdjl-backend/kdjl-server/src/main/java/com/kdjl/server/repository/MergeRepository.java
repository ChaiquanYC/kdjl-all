package com.kdjl.server.repository;

import com.kdjl.common.entity.Merge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MergeRepository extends JpaRepository<Merge, Long> {
    List<Merge> findByAidAndBid(Integer aid, Integer bid);
}
