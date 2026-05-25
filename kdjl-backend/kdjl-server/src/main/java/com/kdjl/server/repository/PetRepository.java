package com.kdjl.server.repository;

import com.kdjl.common.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByName(String name);
    List<Pet> findByWx(Integer element);
}
