package com.marcel.parking.entity;

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
        name = "garage_sector",
        uniqueConstraints = @UniqueConstraint(name = "uk_garage_sector_sector", columnNames = "sector")
)
public class GarageSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Integer occupiedCount = 0;

    @Column(nullable = false)
    private Boolean closed = false;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    public GarageSector(String sector, BigDecimal basePrice, Integer maxCapacity) {
        this.sector = sector;
        this.basePrice = basePrice;
        this.maxCapacity = maxCapacity;
    }

    public BigDecimal occupancyRate() {
        if (maxCapacity == null || maxCapacity == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(occupiedCount)
                .divide(BigDecimal.valueOf(maxCapacity), 4, java.math.RoundingMode.HALF_UP);
    }

    public boolean isFull() {
        return occupiedCount >= maxCapacity;
    }

    public void incrementOccupancy() {
        this.occupiedCount++;
        this.closed = isFull();
    }

    public void decrementOccupancy() {
        if (this.occupiedCount > 0) {
            this.occupiedCount--;
        }
        this.closed = isFull();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
