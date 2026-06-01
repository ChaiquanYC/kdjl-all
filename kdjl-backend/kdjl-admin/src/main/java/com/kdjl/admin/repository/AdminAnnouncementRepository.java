package com.kdjl.admin.repository;

import com.kdjl.common.entity.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE (:kw = '' OR a.msg LIKE CONCAT('%', :kw, '%')) AND (:status IS NULL OR a.status = :status) ORDER BY a.sortOrder ASC, a.id DESC")
    List<Announcement> search(@Param("kw") String keyword, @Param("status") Integer status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE (:kw = '' OR a.msg LIKE CONCAT('%', :kw, '%')) AND (:status IS NULL OR a.status = :status)")
    long countByKeywordAndStatus(@Param("kw") String keyword, @Param("status") Integer status);
}
