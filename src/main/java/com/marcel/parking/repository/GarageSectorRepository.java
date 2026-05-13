package com.marcel.parking.repository;

import com.marcel.parking.entity.GarageSector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarageSectorRepository extends JpaRepository<GarageSector, Long> {

    Optional<GarageSector> findBySector(String sector);

    Optional<GarageSector> findWithLockBySector(String sector);

    boolean existsBySector(String sector);
}
