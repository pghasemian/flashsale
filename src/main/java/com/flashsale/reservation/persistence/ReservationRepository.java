package com.flashsale.reservation.persistence;

import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByStatus(ReservationStatus status);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Reservation r
        SET r.status = :newStatus
        WHERE r.id = :reservationId
          AND r.status = :currentStatus
        """)
    int updateStatusIfCurrent(
            @Param("reservationId") Long reservationId,
            @Param("currentStatus") ReservationStatus currentStatus,
            @Param("newStatus") ReservationStatus newStatus
    );

    List<Reservation> findByStatusAndExpiresAtBefore(
            ReservationStatus status,
            Instant expiresAt
    );
}