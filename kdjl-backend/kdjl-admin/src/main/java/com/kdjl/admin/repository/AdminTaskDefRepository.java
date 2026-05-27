package com.kdjl.admin.repository;

import com.kdjl.common.entity.TaskDef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTaskDefRepository extends JpaRepository<TaskDef, Long> {
    List<TaskDef> findByTitleContaining(String title);

    @Query("SELECT t FROM TaskDef t WHERE (:kw = '' OR t.title LIKE CONCAT('%', :kw, '%')) AND (:color IS NULL OR t.color = :color)")
    List<TaskDef> searchByKeywordAndColor(@Param("kw") String keyword, @Param("color") Integer color, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TaskDef t WHERE (:kw = '' OR t.title LIKE CONCAT('%', :kw, '%')) AND (:color IS NULL OR t.color = :color)")
    long countByKeywordAndColor(@Param("kw") String keyword, @Param("color") Integer color);

    @Query(value = "SELECT MAX(CAST(cid AS UNSIGNED)) FROM task WHERE cid REGEXP '^[0-9]+$'", nativeQuery = true)
    Integer findMaxNumericCid();

    @Query("SELECT MAX(t.xulie) FROM TaskDef t WHERE t.cid = :cid")
    Integer findMaxXulieByCid(@Param("cid") String cid);
}
