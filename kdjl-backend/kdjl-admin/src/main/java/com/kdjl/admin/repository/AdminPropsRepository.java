package com.kdjl.admin.repository;

import com.kdjl.common.entity.Props;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminPropsRepository extends JpaRepository<Props, Long> {
    Optional<Props> findByName(String name);
}
