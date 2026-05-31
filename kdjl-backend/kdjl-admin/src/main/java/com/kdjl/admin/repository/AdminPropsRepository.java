package com.kdjl.admin.repository;

import com.kdjl.common.entity.Props;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminPropsRepository extends JpaRepository<Props, Long> {
    Optional<Props> findByName(String name);

    @Query("SELECT p FROM Props p WHERE :kw = '' OR p.name LIKE CONCAT('%', :kw, '%')")
    Page<Props> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Props p WHERE :kw = '' OR p.name LIKE CONCAT('%', :kw, '%')")
    long countByKeyword(@Param("kw") String keyword);

    @Query("SELECT p FROM Props p WHERE :kw = '' OR p.name LIKE CONCAT('%', :kw, '%')")
    List<Props> searchByName(@Param("kw") String keyword, Pageable pageable);
}
