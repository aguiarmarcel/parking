package com.marcel.parking.entity;

import com.marcel.parking.enumtype.ParkingTicketStatus;
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
        name = "parking_ticket",
        indexes = {
                @Index(name = "idx_ticket_license_status", columnList = "licensePlate,status"),
                @Index(name = "idx_ticket_sector_exit_time", columnList = "sector,exitTime"),
                @Index(name = "idx_ticket_exit_time", columnList = "exitTime")
        }
)
public class ParkingTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String licensePlate;

    @Column(nullable = false, length = 50)
    private String sector;

    private Long spotId;

    @Column(nullable = false)
    private Instant entryTime;

    private Instant parkedTime;

    private Instant exitTime;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal priceMultiplier;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParkingTicketStatus status = ParkingTicketStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    public ParkingTicket(
            String licensePlate,
            String sector,
            Long spotId,
            Instant entryTime,
            BigDecimal priceMultiplier
    ) {
        this.licensePlate = licensePlate;
        this.sector = sector;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.priceMultiplier = priceMultiplier;
        this.status = ParkingTicketStatus.OPEN;
    }

    public void markAsParked(Long spotId) {
        this.spotId = spotId;
        this.parkedTime = Instant.now();
        this.status = ParkingTicketStatus.PARKED;
    }

    public void close(Instant exitTime, BigDecimal amount) {
        this.exitTime = exitTime;
        this.amount = amount;
        this.status = ParkingTicketStatus.CLOSED;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
