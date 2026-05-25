package com.kdjl.admin.repository;

import com.kdjl.common.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminPetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByName(String name);
}
