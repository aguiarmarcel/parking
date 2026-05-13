package com.marcel.parking.entity;

import com.marcel.parking.enumtype.ParkingSpotStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "parking_spot",
        indexes = {
                @Index(name = "idx_parking_spot_sector_status", columnList = "sector,status"),
                @Index(name = "idx_parking_spot_license_plate", columnList = "occupiedByLicensePlate")
        }
)
public class ParkingSpot {
    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParkingSpotStatus status = ParkingSpotStatus.AVAILABLE;

    @Column(length = 20)
    private String occupiedByLicensePlate;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    public ParkingSpot(Long id, String sector, BigDecimal latitude, BigDecimal longitude) {
        this.id = id;
        this.sector = sector;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = ParkingSpotStatus.AVAILABLE;
    }

    public void occupy(String licensePlate) {
        this.status = ParkingSpotStatus.OCCUPIED;
        this.occupiedByLicensePlate = licensePlate;
    }

    public void release() {
        this.status = ParkingSpotStatus.AVAILABLE;
        this.occupiedByLicensePlate = null;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
