package com.kdjl.admin.repository;

import com.kdjl.common.entity.AuctionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuctionLogRepository extends JpaRepository<AuctionLog, Long> {
    @Query("SELECT l FROM AuctionLog l WHERE (:sellerId IS NULL OR l.sellerId = :sellerId) AND (:buyerId IS NULL OR l.buyerId = :buyerId) AND (:action = '' OR l.action = :action) ORDER BY l.createdAt DESC")
    Page<AuctionLog> searchLogs(@Param("sellerId") Integer sellerId, @Param("buyerId") Integer buyerId, @Param("action") String action, Pageable pageable);
}
