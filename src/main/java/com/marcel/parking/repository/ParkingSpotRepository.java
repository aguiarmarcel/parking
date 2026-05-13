package com.marcel.parking.repository;

import com.marcel.parking.entity.ParkingSpot;
import com.marcel.parking.enumtype.ParkingSpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    Optional<ParkingSpot> findFirstBySectorAndStatusOrderByIdAsc(String sector, ParkingSpotStatus status);

    Optional<ParkingSpot> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);

    Optional<ParkingSpot> findByOccupiedByLicensePlate(String licensePlate);
}
