package com.marcel.parking.repository;

import com.marcel.parking.entity.ParkingTicket;
import com.marcel.parking.enumtype.ParkingTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    Optional<ParkingTicket> findFirstByLicensePlateAndStatusInOrderByEntryTimeDesc(
            String licensePlate,
            Iterable<ParkingTicketStatus> statuses
    );

    Optional<ParkingTicket> findFirstByLicensePlateAndStatusOrderByEntryTimeDesc(
            String licensePlate,
            ParkingTicketStatus status
    );

    @Query("""
            select coalesce(sum(t.amount), 0)
            from ParkingTicket t
            where t.sector = :sector
              and t.status = com.marcel.parking.enumtype.ParkingTicketStatus.CLOSED
              and t.exitTime >= :start
              and t.exitTime < :end
            """)
    BigDecimal sumRevenueBySectorAndExitTimeBetween(String sector, Instant start, Instant end);
}
