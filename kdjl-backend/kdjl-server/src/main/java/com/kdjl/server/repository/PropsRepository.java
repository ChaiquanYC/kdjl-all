package com.kdjl.server.repository;

import com.kdjl.common.entity.Props;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropsRepository extends JpaRepository<Props, Long> {
    Optional<Props> findByName(String name);
    List<Props> findByVary(Integer vary);
}
