package com.kdjl.server.repository;

import com.kdjl.common.entity.CardToTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardToTitleRepository extends JpaRepository<CardToTitle, Integer> {
}