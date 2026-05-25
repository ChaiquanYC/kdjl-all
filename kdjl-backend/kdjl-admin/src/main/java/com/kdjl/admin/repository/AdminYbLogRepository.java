package com.kdjl.admin.repository;

import com.kdjl.common.entity.YbLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminYbLogRepository extends JpaRepository<YbLog, Long> {
    List<YbLog> findTop100ByOrderByBuytimeDesc();

    @Query("SELECT y.title, SUM(y.yb) as total FROM YbLog y GROUP BY y.title ORDER BY total DESC")
    List<Object[]> sumByTitle();
}
